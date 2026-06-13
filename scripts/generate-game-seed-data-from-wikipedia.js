const fs = require('fs');
const path = require('path');
const https = require('https');

const ROOT = path.join(__dirname, '..');
const OUT_DIR = path.join(ROOT, 'prisma', 'seed-data', 'games');
const TARGET = 800;

const platforms = [
  { file: 'switch.json', platform: 'Switch', pages: ['List of Nintendo Switch games (0-9)', 'List of Nintendo Switch games (A-Am)', 'List of Nintendo Switch games (An-Az)', 'List of Nintendo Switch games (B)', 'List of Nintendo Switch games (C-G)', 'List of Nintendo Switch games (H-P)', 'List of Nintendo Switch games (Q-Z)'] },
  { file: 'ps5.json', platform: 'PS5', pages: ['List of PlayStation 5 games'] },
  { file: 'ps4.json', platform: 'PS4', pages: ['List of PlayStation 4 games (A–L)', 'List of PlayStation 4 games (M–Z)'] },
  { file: 'ps3.json', platform: 'PS3', pages: ['List of PlayStation 3 games (A–C)', 'List of PlayStation 3 games (D–I)', 'List of PlayStation 3 games (J–P)', 'List of PlayStation 3 games (Q–Z)'] },
  { file: 'ps2.json', platform: 'PS2', pages: ['List of PlayStation 2 games (A–K)', 'List of PlayStation 2 games (L–Z)'] },
  { file: 'ps1.json', platform: 'PS1', pages: ['List of PlayStation (console) games (A–L)', 'List of PlayStation (console) games (M–Z)'] },
  { file: 'psp.json', platform: 'PSP', pages: ['List of PlayStation Portable games'] },
  { file: 'xbox_360.json', platform: 'Xbox 360', pages: ['List of Xbox 360 games (A–L)', 'List of Xbox 360 games (M–Z)'] },
  { file: 'xbox.json', platform: 'Xbox', pages: ['List of Xbox games'] },
  { file: 'wii.json', platform: 'Wii', pages: ['List of Wii games'] },
  { file: 'gamecube.json', platform: 'GameCube', pages: ['List of GameCube games'] },
  { file: 'gba.json', platform: 'GBA', pages: ['List of Game Boy Advance games'] },
  { file: 'ds.json', platform: 'DS', pages: ['List of Nintendo DS games (0–C)', 'List of Nintendo DS games (D–I)', 'List of Nintendo DS games (J–P)', 'List of Nintendo DS games (Q–Z)'] },
  { file: '3ds.json', platform: '3DS', pages: ['List of Nintendo 3DS games (0–M)', 'List of Nintendo 3DS games (N–Z)'] },
  { file: 'n64.json', platform: 'N64', pages: ['List of Nintendo 64 games'] },
  { file: 'nes.json', platform: 'NES', pages: ['List of Nintendo Entertainment System games'] },
  { file: 'snes.json', platform: 'SNES', pages: ['List of Super Nintendo Entertainment System games'] },
  { file: 'genesis.json', platform: 'Genesis', pages: ['List of Sega Genesis games'] },
  { file: 'dreamcast.json', platform: 'Dreamcast', pages: ['List of Dreamcast games'] },
  { file: 'saturn.json', platform: 'Saturn', pages: ['List of Sega Saturn games'] },
  { file: 'game_boy.json', platform: 'Game Boy', pages: ['List of Game Boy games'] },
  { file: 'game_boy_color.json', platform: 'Game Boy Color', pages: ['List of Game Boy Color games'] },
  { file: 'virtual_boy.json', platform: 'Virtual Boy', pages: ['List of Virtual Boy games'] },
  { file: 'atari_2600.json', platform: 'Atari 2600', pages: ['List of Atari 2600 games'] },
  { file: 'atari_7800.json', platform: 'Atari 7800', pages: ['List of Atari 7800 games'] },
  { file: 'jaguar.json', platform: 'Jaguar', pages: ['List of Atari Jaguar games'] },
  { file: 'lynx.json', platform: 'Lynx', pages: ['List of Atari Lynx games'] }
];

const collectorAllowSports = ['NCAA Football 14', 'NBA Street', 'Def Jam', 'Tony Hawk', 'SSX', 'Mario Tennis', 'Mario Golf', 'Mario Strikers', 'Mario Superstar Baseball', 'Punch-Out', 'Rocket League', 'Skate', 'Snowboard Kids', 'Wave Race', '1080', 'F-Zero', 'Excitebike', 'California Games', 'Ninja Golf', 'Mario Kart', 'Diddy Kong Racing', 'Crash Team Racing', 'Sonic Racing', 'Gran Turismo', 'Forza Horizon', 'Crazy Taxi'];
const sportsTerms = ['FIFA', 'Madden', 'NBA 2K', 'NBA Live', 'NHL ', 'MLB ', 'PGA ', 'Tiger Woods', 'WWE ', 'WWF ', 'Pro Evolution Soccer', 'Winning Eleven', 'Football Manager', 'Baseball', 'Basketball', 'Soccer', 'Football', 'Hockey', 'Golf', 'Tennis', 'Cricket', 'Rugby', 'Olympic', 'Olympics', 'Fishing', 'Bowling', 'Billiards', 'Pool', 'Snooker', 'Motocross', 'Supercross', 'NASCAR', 'Formula One', 'F1 ', 'MotoGP', 'UFC ', 'Boxing', 'Wrestling'];
const titleBlocklist = ['List of', 'Cancelled', 'Games', 'Title', 'Developer', 'Publisher', 'Reference', 'Notes', 'Downloadable content', 'Expansion', 'Demo', 'Application', 'System software'];
const popularTerms = ['Mario', 'Zelda', 'Pokemon', 'Metroid', 'Kirby', 'Fire Emblem', 'God of War', 'Halo', 'Sonic', 'Castlevania', 'Resident Evil', 'Silent Hill', 'Persona', 'Final Fantasy', 'Dragon Quest', 'Metal Gear', 'Mega Man', 'Chrono', 'EarthBound', 'Xenoblade', 'Shin Megami Tensei', 'Suikoden', 'Rule of Rose', 'Kuon', 'Haunting Ground', 'Panzer Dragoon Saga', 'Cubivore', 'Gotcha Force', 'Path of Radiance', 'Skies of Arcadia', 'Marvel vs. Capcom 2'];

const franchiseGenres = [
  [/Pokemon|Dragon Quest|Final Fantasy|Persona|Shin Megami|Xenoblade|Tales of|Ys |Suikoden|Chrono|EarthBound|Golden Sun|Fire Emblem|Disgaea|Star Ocean|Kingdom Hearts|Ni no Kuni|Octopath|Bravely|Phantasy Star|Xenosaga|Lunar/i, 'RPG'],
  [/Zelda|Metroid|Castlevania|Okami|Ico|Shadow of the Colossus|Tomb Raider|Uncharted|God of War|Metal Gear|Assassin|Darksiders|Bayonetta|Devil May Cry|Ninja Gaiden/i, 'Adventure'],
  [/Mario|Sonic|Kirby|Crash|Spyro|Rayman|Mega Man|Donkey Kong|Yoshi|Banjo|Klonoa|Shantae|Wario Land|Ape Escape|LittleBigPlanet|Ratchet|Sly Cooper/i, 'Platformer'],
  [/Resident Evil|Silent Hill|Fatal Frame|Dino Crisis|Outlast|Until Dawn|Alan Wake|Dead Space|Evil Within|Clock Tower|Alone in the Dark|Rule of Rose|Haunting Ground|Kuon/i, 'Horror'],
  [/Street Fighter|Tekken|Mortal Kombat|Soulcalibur|King of Fighters|Guilty Gear|BlazBlue|Marvel vs|Capcom vs|Smash|Virtua Fighter|Dead or Alive|Killer Instinct|Power Stone/i, 'Fighting'],
  [/Halo|Call of Duty|Doom|Quake|Wolfenstein|Battlefield|Gears of War|Killzone|Resistance|BioShock|Borderlands|Destiny|Splatoon|Perfect Dark|GoldenEye|Half-Life|Far Cry/i, 'Shooter'],
  [/Advance Wars|Civilization|XCOM|Ogre Battle|Tactics|Command & Conquer|Warcraft|StarCraft|Pikmin|Triangle Strategy|Valkyria/i, 'Strategy'],
  [/Mario Kart|Gran Turismo|Forza|Need for Speed|Burnout|F-Zero|Ridge Racer|Wipeout|Diddy Kong Racing|Crash Team Racing|Crazy Taxi|Wave Race/i, 'Racing'],
  [/Animal Crossing|Harvest Moon|Story of Seasons|Stardew|The Sims|SimCity|RollerCoaster|Theme Park|Nintendogs|Seaman/i, 'Simulation'],
  [/Tetris|Puyo|Dr. Mario|Puzzle|Picross|Lumines|Professor Layton|Meteos/i, 'Puzzle'],
  [/Guitar Hero|Rock Band|Dance Dance|Just Dance|PaRappa|Rhythm|Elite Beat|Beat Saber|Taiko/i, 'Simulation'],
  [/Minecraft|Terraria|Roblox|Dreams/i, 'Simulation']
];

function sleep(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }
function fetchRaw(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'CollectosSeedGenerator/1.0 (student project)' } }, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}
async function fetchPage(page, attempt = 1) {
  const url = `https://en.wikipedia.org/w/api.php?action=parse&page=${encodeURIComponent(page)}&prop=text&format=json&origin=*`;
  const data = await fetchRaw(url);
  if (!data.trim().startsWith('{')) {
    if (attempt < 8) { await sleep(data.includes('too many requests') ? 30000 : 3000 * attempt); return fetchPage(page, attempt + 1); }
    throw new Error(`${page}: non-JSON response from Wikipedia: ${data.slice(0, 80)}`);
  }
  const json = JSON.parse(data);
  if (json.error) throw new Error(`${page}: ${json.error.info}`);
  await sleep(900);
  return json.parse.text['*'];
}
function decodeEntities(text) {
  return text.replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#039;|&apos;/g, "'").replace(/&ndash;|&mdash;/g, '-').replace(/&nbsp;/g, ' ').replace(/&#160;/g, ' ').replace(/&#\d+;/g, ' ');
}
function stripHtml(html) {
  return decodeEntities(html.replace(/<style[\s\S]*?<\/style>/gi, ' ').replace(/<script[\s\S]*?<\/script>/gi, ' ').replace(/<sup[\s\S]*?<\/sup>/gi, ' ').replace(/<span class="sortkey"[\s\S]*?<\/span>/gi, ' ').replace(/<br\s*\/?>/gi, ' / ').replace(/<[^>]+>/g, ' ')).replace(/\[[^\]]*\]/g, ' ').replace(/\s+/g, ' ').trim();
}
function extractRows(html) {
  const rows = [];
  const tableMatches = html.match(/<table[\s\S]*?<\/table>/gi) || [];
  for (const table of tableMatches) {
    if (!/(wikitable|sortable|plainrowheaders)/i.test(table)) continue;
    const trMatches = table.match(/<tr[\s\S]*?<\/tr>/gi) || [];
    for (const tr of trMatches) {
      const cellMatches = tr.match(/<(td|th)[\s\S]*?<\/\1>/gi) || [];
      if (cellMatches.length < 2) continue;
      const cells = cellMatches.map(stripHtml).filter(Boolean);
      if (cells.length >= 2) rows.push(cells);
    }
  }
  return rows;
}
function cleanTitle(raw) {
  let title = raw.replace(/\s*\(video game\)\s*/gi, '').replace(/\s*\^.*$/g, '').replace(/\s+/g, ' ').trim();
  if (title.includes(' / ')) title = title.split(' / ')[0].trim();
  if (title.includes(' •')) title = title.split(' •')[0].trim();
  return title.replace(/^"|"$/g, '').trim();
}
function isTitleCandidate(title) {
  if (!title || title.length < 2 || title.length > 90) return false;
  if (/^\d{4}$/.test(title) || /^[A-Z]{1,3}$/.test(title)) return false;
  if (titleBlocklist.some((bad) => title.toLowerCase() === bad.toLowerCase())) return false;
  if (titleBlocklist.some((bad) => title.toLowerCase().startsWith(`${bad.toLowerCase()} `))) return false;
  return true;
}
function isAllowedSports(title) { return collectorAllowSports.some((term) => title.toLowerCase().includes(term.toLowerCase())); }
function isGenericSports(title) { return !isAllowedSports(title) && sportsTerms.some((term) => title.toLowerCase().includes(term.trim().toLowerCase())); }
function inferYear(cells) {
  const years = [...cells.join(' ').matchAll(/\b(19[7-9]\d|20[0-2]\d)\b/g)].map((m) => Number(m[1]));
  return years.length ? Math.min(...years) : 2000;
}
function inferGenre(title) {
  for (const [pattern, genre] of franchiseGenres) if (pattern.test(title)) return genre;
  return 'Action';
}
function valuesFor(year, title) {
  let values = year < 2013 ? { looseValue: 20, cibValue: 40, newValue: 80 } : year <= 2017 ? { looseValue: 10, cibValue: 20, newValue: 35 } : year <= 2021 ? { looseValue: 15, cibValue: 28, newValue: 45 } : { looseValue: 25, cibValue: 40, newValue: 65 };
  const bump = popularTerms.some((term) => title.toLowerCase().includes(term.toLowerCase())) || isAllowedSports(title);
  const mult = bump ? 1.5 : 1;
  return { looseValue: +(values.looseValue * mult).toFixed(2), cibValue: +(values.cibValue * mult).toFixed(2), newValue: +(values.newValue * mult).toFixed(2) };
}
function scoreTitle(title) {
  let score = 0;
  if (popularTerms.some((term) => title.toLowerCase().includes(term.toLowerCase()))) score += 100;
  if (isAllowedSports(title)) score += 75;
  if (/\b(remaster|collection|ultimate|deluxe|complete|definitive|limited|collector)\b/i.test(title)) score += 20;
  if (/\b(Barbie|Dora|Nickelodeon|SpongeBob|Disney|Bratz|Petz|Imagine|Mary-Kate|Lizzie McGuire)\b/i.test(title)) score -= 25;
  if (/\b(2|3|4|V|VI|VII|VIII|IX|X)\b/.test(title)) score += 5;
  return score;
}
function buildItems(rows, platform) {
  const byTitle = new Map();
  for (const cells of rows) {
    const title = cleanTitle(cells[0]);
    if (!isTitleCandidate(title) || isGenericSports(title)) continue;
    const releaseYear = inferYear(cells);
    const genre = inferGenre(title);
    byTitle.set(title.toLowerCase(), { type: 'GAME', title, platform, upc: null, ...valuesFor(releaseYear, title), genre, releaseYear, searchableText: `${title} ${platform} ${genre}` });
  }
  return [...byTitle.values()].sort((a, b) => scoreTitle(b.title) - scoreTitle(a.title) || a.title.localeCompare(b.title)).slice(0, TARGET).sort((a, b) => a.title.localeCompare(b.title));
}
async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true });
  let total = 0;
  const summary = [];
  for (const config of platforms) {
    process.stdout.write(`Fetching ${config.platform}... `);
    const parts = [];
    for (const page of config.pages) parts.push(await fetchPage(page));
    const rows = parts.flatMap(extractRows);
    const items = buildItems(rows, config.platform);
    fs.writeFileSync(path.join(OUT_DIR, config.file), JSON.stringify(items, null, 2) + '\n');
    total += items.length;
    summary.push({ file: config.file, platform: config.platform, count: items.length, sourceRows: rows.length, sourcePages: config.pages });
    console.log(`${items.length} items (${rows.length} source rows)`);
  }
  fs.writeFileSync(path.join(OUT_DIR, '_summary.json'), JSON.stringify({ generatedAt: new Date().toISOString(), targetPerPlatform: TARGET, total, platforms: summary }, null, 2) + '\n');
  console.log(`TOTAL ${total}`);
}
main().catch((err) => { console.error(err); process.exit(1); });

