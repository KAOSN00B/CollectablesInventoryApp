require('dotenv/config');
const fs = require('fs');
const path = require('path');

const RAWG_API_KEY = process.env.RAWG_API_KEY;
const ROOT = path.join(__dirname, '..');
const SEED_DIR = path.join(ROOT, 'prisma', 'seed-data', 'games');
const MAX_ITEMS = Number(process.env.MAX_COVER_ART_ITEMS || '250');
const DELAY_MS = Number(process.env.COVER_ART_DELAY_MS || '1200');

if (!RAWG_API_KEY) {
  console.error('RAWG_API_KEY is required. Add it to .env or pass it in the environment.');
  process.exit(1);
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function gamePriority(game) {
  const title = String(game.title || '');
  const platform = String(game.platform || '');
  let score = 0;

  if (/^(Super Mario|Mario|The Legend of Zelda|Zelda|Pokemon|Pokémon|Sonic|Halo|God of War|Final Fantasy|Metroid|Castlevania|Kirby|Donkey Kong|Resident Evil|Silent Hill|Mega Man|Street Fighter|Mortal Kombat|Chrono|EarthBound)/i.test(title)) score += 1000;
  if (/^(NES|SNES|N64|GameCube|Game Boy|Game Boy Color|GBA|PS1|PS2|Genesis|Dreamcast|Saturn)$/i.test(platform)) score += 200;
  score += Number(game.cibValue || 0);
  return score;
}

function readPlatformFiles() {
  return fs.readdirSync(SEED_DIR)
    .filter((file) => file.endsWith('.json') && !file.startsWith('_'))
    .sort()
    .map((file) => {
      const filePath = path.join(SEED_DIR, file);
      const games = JSON.parse(fs.readFileSync(filePath, 'utf8'));
      return { file, filePath, games };
    });
}

async function findCoverUrl(title, platform) {
  const query = `${title} ${platform}`;
  const url = new URL('https://api.rawg.io/api/games');
  url.searchParams.set('key', RAWG_API_KEY);
  url.searchParams.set('search', query);
  url.searchParams.set('page_size', '1');

  const response = await fetch(url, {
    headers: {
      'User-Agent': 'Collectos catalog cover enrichment',
      'Accept': 'application/json'
    }
  });

  if (!response.ok) {
    throw new Error(`RAWG ${response.status} for ${query}`);
  }

  const body = await response.json();
  return body.results?.[0]?.background_image || null;
}

async function main() {
  const files = readPlatformFiles();
  const candidates = [];

  for (const platformFile of files) {
    platformFile.games.forEach((game, index) => {
      if (game.type === 'GAME' && !game.imageUrl) {
        candidates.push({ platformFile, game, index, priority: gamePriority(game) });
      }
    });
  }

  candidates.sort((a, b) => b.priority - a.priority);
  const selected = candidates.slice(0, MAX_ITEMS);
  console.log(`Found ${candidates.length} games missing imageUrl. Enriching ${selected.length}.`);

  let updated = 0;
  for (const candidate of selected) {
    const { game } = candidate;
    try {
      const imageUrl = await findCoverUrl(game.title, game.platform);
      if (imageUrl) {
        game.imageUrl = imageUrl;
        updated += 1;
        console.log(`  + ${game.title} (${game.platform})`);
      } else {
        console.log(`  - no image: ${game.title} (${game.platform})`);
      }
    } catch (error) {
      console.log(`  ! ${game.title} (${game.platform}): ${error.message}`);
    }
    await sleep(DELAY_MS);
  }

  for (const platformFile of files) {
    fs.writeFileSync(platformFile.filePath, `${JSON.stringify(platformFile.games, null, 2)}\n`);
  }

  console.log(`Done. Added ${updated} imageUrl values.`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});