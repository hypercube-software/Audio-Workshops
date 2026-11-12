=== K2600R Memory map

Here the result of my investigation, trying to brute-force all the object ID and all the banks of my K2600R using DIR_BANK command.
- According to the .H, types goes from 96 to 159 but there are more.
- Suprisingly the "All Bank" *7F*, does not always include the right count !
Here those who are not empty:

#figure(
  table(
    align: (left, left, center, center, center, center, center, center, center, center, center, center, center),
    columns: (auto, auto, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr, 1fr),
    fill: (x, y) => if y == 0 { luma(230) },
    stroke: 0.5pt,
    table.header([*Type*], [*Name*], [*Bank 0*], [*Bank 1*], [*Bank 2*], [*Bank 3*], [*Bank 4*], [*Bank 5*], [*Bank 6*], [*Bank 7*], [*Bank 8*], [*Bank 9*], [*Bank 7F*]),
    [97], [INDEX], [], [], [14], [4], [6], [], [], [], [], [], [24],
    [102], [ALG_DESC], [98], [27], [], [], [], [], [], [], [], [], [125],
    [103], [INTONATION_TABLE], [20], [], [], [], [], [], [], [], [], [], [],
    [104], [VELOCITY_MAP], [9], [], [], [], [], [], [], [], [], [], [],
    [105], [PRESSURE_MAP], [7], [], [], [], [], [], [], [], [], [], [],
    [106], [EDIT], [14], [], [], [], [], [], [], [], [], [], [],
    [107], [EDIT_MENU], [65], [18], [18], [20], [18], [20], [20], [20], [20], [5], [170],
    [108], [MENU_GROUP], [15], [], [], [], [], [], [], [], [], [], [],
    [109], [MENU_ENTRY_LIST], [65], [18], [18], [20], [8], [10], [16], [16], [3], [5], [125],
    [111], [B_LIST], [9], [], [], [], [], [], [], [], [10], [10], [20],
    [112], [SONG], [50], [], [], [], [], [], [], [], [], [], [],
    [114], [SHAPE], [26], [], [], [], [], [], [], [], [], [], [],
    [115], [F_PROC], [51], [], [], [], [], [], [], [], [], [], [12],
    [132], [PROGRAM], [99], [100], [3], [], [], [], [], [60], [100], [100], [462],
    [133], [KEYMAP], [97], [93], [2], [], [], [], [], [9], [94], [91], [386],
    [134], [SOUND_BLOCK], [67], [81], [], [], [], [], [], [18], [94], [63], [323],
    [135], [SETUP], [1], [], [], [], [], [], [], [], [], [], [1],
    [137], [SETUP_2], [99], [], [], [], [], [], [], [], [51], [51], [201],
    [140], [STUDIO], [99], [100], [], [], [], [], [], [20], [71], [], [290],
    [141], [KDFX_PRESET], [75], [100], [], [], [], [], [], [100], [71], [100], [446],
    [142], [DITHER], [16], [1], [], [], [], [], [], [], [], [], [],
    [143], [KDFX_ALG], [15], [22], [], [], [], [], [], [51], [], [49], [137],
    [144], [STUDIO_PARAM], [14], [], [], [], [], [], [], [], [], [], [14]
  ),
  caption: [Types and banks for K2600R],
)
