# Collectos UI Mockups

These mockups describe the implemented Android XML direction: single Activity, Navigation Component, MaterialToolbar, BottomNavigationView, dark Collector Vault styling.

## 1. Onboarding

```text
[full screen, no toolbar, no bottom nav]

COLLECTOS
Track games, cards, comics, toys, and consoles without personal accounts.

Collection name (optional)
[ Robert's Vault                         ]
[ Create Anonymous Collection             ]

Have a collection code?
[ ABC12                                  ]
[ Join Collection                         ]

(progress indicator appears while connecting)
```

Behavior: first launch only. Existing `public_code` skips to Home.

## 2. Home / Dashboard

```text
Toolbar: Home                         Bottom nav: Home selected

[ Total Collection Value              ]
[ $2,480.00                           ]
[ 86 items in collection              ]

[ Scan ] [ Search ] [ Add ]

By Category
Games          $1,420.00
42 games
Consoles       $360.00
6 consoles
Comics         $240.00
12 comics
Trading Cards  $300.00
20 cards
Toys / Figures $120.00
4 toys
LEGO           $40.00
2 sets

4 items on wishlist

Recently Added Games
Pokemon Emerald (GBA) - $180.00
Silent Hill 2 (PS2) - $30.00
```

Behavior: top-level, no back arrow. Quick actions use NavController/global actions.

## 3. Search / Price Check

```text
Toolbar: Search                       Bottom nav: Search selected

Price Check
Standing in a store? Look up the market price and see if the deal is worth it.

[ Item name (e.g. Mario Kart Switch)       ]
[ Store asking price ($) - optional        ]
[ Search                                   ]

Result card after search:
Pokemon Emerald Version
GBA
Loose: $180.00
CIB: $300.00
New: $600.00
GOOD DEAL / OVER MARKET appears if asking price was entered

[ Add to My Collection ]
[ Add to Wishlist      ]
```

Behavior: top-level, no back arrow. Barcode scanner opens as child with back arrow.

## 4. Search Results

```text
Toolbar: Search                       Bottom nav: Search selected

[ pokemon emerald                         ]

Likely Matches
[ GAME ] Pokemon Emerald Version      $180.00
       GBA - RPG

[ GAME ] Pokemon Ruby Version         $30.00
       GBA - RPG

[ GAME ] Pokemon Sapphire Version     $30.00
       GBA - RPG
```

Behavior: selecting a result can prefill add/edit flow later. Current MVP uses the existing result card path.

## 5. Collection List

```text
Toolbar: Collection                   Bottom nav: Collection selected

[All] [Games] [Consoles] [Comics] [TCG] [Toys] [LEGO]
86 items - $2,480.00 estimated value

[GAME] Pokemon Emerald Version        $180.00
       GBA
       Good - Loose

[CONS] Nintendo GameCube              $95.00
       Nintendo - DOL-001
       Good

[TCG ] Charizard                      $300.00
       Pokemon
       Near Mint

                                      (+)
```

Behavior: top-level, no back arrow. FAB opens Smart Add. Chips filter one unified list.

Empty state:

```text
No items here yet
Use Smart Add to scan, search, or enter your first collectible.
[ Add Item ]
```

## 6. Item Detail

```text
Toolbar: Game Details              Back arrow visible
Menu: Edit / Delete

Pokemon Emerald Version
GBA - RPG
Condition: Good
Completeness: Loose
Purchase Price: $80.00
Estimated Value: $180.00
Notes: Clean label, authentic cart.
```

Behavior: child screen. Toolbar/device back returns to Collection or previous source.

## 7. Add/Edit Item

```text
Toolbar: Add / Edit Game           Back arrow visible

Title
[ Pokemon Emerald Version              ]
Platform
[ GBA                                  ]
Genre
[ RPG                                  ]
Condition
[ Good                                 ]
Purchase Price
[ 80.00                                ]
Estimated Value
[ 180.00                               ]
Notes
[ Clean label, authentic cart          ]

[ Save ]
```

Behavior: existing category-specific forms remain. Catalog/barcode prefill can populate fields later.

## 8. Smart Add Bottom Sheet

```text
[bottom sheet]

Smart Add

[ camera ] Scan Barcode
           Point camera at a barcode to auto-fill the form

[ search ] Search Catalog
           Type a title or name to look up values and fill the form

[ plus   ] Manual Entry
           Fill in the details yourself
```

Manual Entry from Home/Collection opens a category picker:

```text
Choose item type
Game
Console
Comic / TCG / Toy / LEGO
```

## 9. Barcode Scanner

```text
Toolbar: Scan Barcode              Back arrow visible

[ camera preview area ]

Point the camera at a barcode.
If no match appears, use manual entry.
```

Behavior: child screen. Back exits scanner.

## 10. Wishlist List

```text
Toolbar: Wishlist                  Bottom nav: Wishlist selected

[ GRAIL ] Suikoden II              Target $120.00
          PS1 - Current $160.00

[       ] Metroid Prime 2           Target $35.00
          GameCube - Current $45.00

                                      (+)
```

Behavior: top-level. FAB opens add/edit wishlist.

## 11. Wishlist Detail

```text
Toolbar: Wishlist Item             Back arrow visible
Menu: Edit / Delete

Suikoden II
PS1
Target Price: $120.00
Current Estimate: $160.00
Grail: Yes
Notes: Buy if complete and clean.
```

## 12. Add/Edit Wishlist

```text
Toolbar: Add / Edit Wishlist Item  Back arrow visible

Title
[ Suikoden II                          ]
Platform
[ PS1                                  ]
Target Price
[ 120.00                               ]
Current Estimated Value
[ 160.00                               ]
Grail
[ x ]
Notes
[ Buy if complete and clean            ]

[ Save ]
```
