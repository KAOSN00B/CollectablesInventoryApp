const fs = require('fs');
const path = require('path');

const seedDir = path.join(__dirname, '..', 'prisma', 'seed-data', 'games');

// Base loose price per platform — reflects real-world collector market
const platformBaseLoose = {
  'Virtual Boy':   35,
  'Jaguar':        22,
  'Lynx':          14,
  'Atari 7800':     9,
  'Atari 2600':     6,
  'Saturn':        22,
  'Dreamcast':     14,
  'Game Boy':      12,
  'Game Boy Color':18,
  'GBA':           18,
  'SNES':          22,
  'N64':           22,
  'Genesis':       12,
  'PS1':           14,
  'PS2':           10,
  'GameCube':      18,
  'Xbox':           8,
  'PS4':           14,
  'PS5':           32,
  'Switch':        24,
};

// Title keywords that bump price — checked case-insensitive
const rarityKeywords = [
  // Very rare / high value
  { terms: ['suikoden ii', 'suikoden 2', 'panzer dragoon saga', 'panzer dragoon rpg',
            'rule of rose', 'haunting ground', 'kuon', 'mother 3', 'shantae',
            'conker', 'cubivore', 'gotcha force', 'chibi-robo', 'drill dozer',
            'ninja five-o', 'steel battalion', 'battlesphere', 'radiant silvergun',
            'burning rangers', 'magic knight rayearth', 'tomba', 'misadventures of tron bonne',
            'einhander', 'vagrant story'], multiplier: 5.5 },
  // Rare / sought after
  { terms: ['chrono trigger', 'earthbound', 'castlevania: symphony', 'super metroid',
            'ogre battle', 'harvest moon 64', 'fire emblem: path', 'baten kaitos origins',
            'skies of arcadia', 'guardian heroes', 'dragon force', 'albert odyssey',
            'xenogears', 'parasite eve', 'lunar', 'klonoa', 'gitaroo',
            'cannon spike', 'illbleed', 'musha', 'beyond oasis', 'comix zone'], multiplier: 4.0 },
  // Premium franchise titles
  { terms: ['pokemon', 'zelda', 'metroid', 'fire emblem', 'earthbound',
            'chrono', 'castlevania', 'mega man x3', 'mega man x2'], multiplier: 3.0 },
  // Popular franchise titles
  { terms: ['mario', 'sonic', 'mega man', 'resident evil', 'silent hill',
            'final fantasy', 'metal gear', 'kirby', 'donkey kong', 'star fox',
            'persona', 'shin megami', 'dragon quest', 'kingdom hearts',
            'golden sun', 'advance wars', 'banjo', 'panzer dragoon',
            'jet grind', 'jet set', 'shenmue', 'xenoblade', 'xenosaga'], multiplier: 2.2 },
  // Solid titles with collector demand
  { terms: ['smash bros', 'god of war', 'halo', 'nier', 'okami', 'ico',
            'shadow of the colossus', 'viewtiful joe', 'f-zero', 'pikmin',
            'animal crossing', 'astral chain', 'cuphead', 'hollow knight',
            'streets of rage', 'gunstar heroes', 'contra', 'castlevania'], multiplier: 1.8 },
];

// Sports / shovelware titles get lower prices
const budgetKeywords = [
  'nba live', 'nfl', 'fifa', 'madden', 'nhl', 'mlb', 'pga tour',
  'bowling', 'golf', 'tennis', 'baseball', 'rugby', 'cricket',
  'barbie', 'bratz', 'dora', 'blues clues', 'wiggles', 'paw patrol',
];

function getRarityMultiplier(title) {
  const lower = title.toLowerCase();
  for (const { terms, multiplier } of rarityKeywords) {
    if (terms.some(t => lower.includes(t))) return multiplier;
  }
  if (budgetKeywords.some(t => lower.includes(t))) return 0.5;
  return 1.0;
}

// Older retro games gain some nostalgia premium, very recent titles lose it
function getYearMultiplier(platform, year) {
  if (!year) return 1.0;
  const retro = ['SNES', 'N64', 'Genesis', 'PS1', 'Saturn', 'Dreamcast',
                 'Game Boy', 'Game Boy Color', 'Atari 2600', 'Atari 7800',
                 'Jaguar', 'Lynx', 'Virtual Boy'];
  if (retro.includes(platform)) {
    if (year < 1990) return 1.4;
    if (year < 1995) return 1.2;
    if (year < 2000) return 1.1;
    return 1.0;
  }
  // Modern platforms: newer games hold value better
  if (year >= 2022) return 1.1;
  if (year >= 2019) return 1.0;
  return 0.9;
}

// Small pseudo-random variance so not every game is exactly the same price
function variance(title) {
  let hash = 0;
  for (let i = 0; i < title.length; i++) {
    hash = ((hash << 5) - hash) + title.charCodeAt(i);
    hash |= 0;
  }
  return 1 + ((Math.abs(hash) % 21) - 10) / 100; // ±10%
}

function round(n) {
  return Math.round(n * 2) / 2; // round to nearest $0.50
}

function reprice(game) {
  const base = platformBaseLoose[game.platform] ?? 12;
  const rarity = getRarityMultiplier(game.title);
  const year = getYearMultiplier(game.platform, game.releaseYear);
  const vary = variance(game.title);

  const loose = Math.max(3, round(base * rarity * year * vary));
  const cib   = Math.max(loose + 2, round(loose * (rarity >= 3 ? 2.2 : 1.8)));
  const newVal = Math.max(cib + 5, round(loose * (rarity >= 3 ? 4.0 : 3.2)));

  return { ...game, looseValue: loose, cibValue: cib, newValue: newVal };
}

const files = fs.readdirSync(seedDir)
  .filter(f => f.endsWith('.json') && !f.startsWith('_'));

let total = 0;
for (const file of files) {
  const filePath = path.join(seedDir, file);
  const games = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
  const repriced = games.map(reprice);
  fs.writeFileSync(filePath, JSON.stringify(repriced, null, 2) + '\n');
  total += repriced.length;
  console.log(`${file}: ${repriced.length} games repriced`);
}

console.log(`\nDone — ${total} total games updated`);

// Show sample prices for well-known titles
const sampleTitles = ['Suikoden II', 'Chrono Trigger', 'Pokemon Emerald Version',
  'Sonic the Hedgehog', 'Madden', 'Rule of Rose'];
console.log('\nSample spot-check:');
for (const file of files) {
  const games = JSON.parse(fs.readFileSync(path.join(seedDir, file), 'utf-8'));
  for (const g of games) {
    if (sampleTitles.some(s => g.title.includes(s.split(' ')[0]) && g.title.toLowerCase().includes(s.toLowerCase().split(' ').slice(-1)[0]))) {
      console.log(`  ${g.title} (${g.platform}): loose $${g.looseValue} / cib $${g.cibValue} / new $${g.newValue}`);
    }
  }
}
