# Settings

Open settings by pressing **▲** from the app row to reach the gear (top-left), then **OK**.
On a setting, **OK** or **◀ ▶** changes it. Everything saves immediately. **Back** leaves.
The panel on the right previews the tile style.

## Look & color

| Setting | Options | Default | What it does |
|---|---|---|---|
| Color theme | Automatic, Winter, Spring, Summer, Foliage, Stick season | Automatic | Highlight color. Automatic follows the TV's date: frost (Dec–Feb), lilac (Mar–May), leaf green (Jun–Aug), red maple (Sep–Oct), copper oak (Nov). |
| Tree color | Match highlight, Classic dark green | Match highlight | Pine-ridge colors follow the season, or stay the original dark green. |
| Scenery | Full pine ridge, Front trees only, None | Full | How much of the tree line is drawn. |
| Background | Spruce green, Night forest, Black | Spruce green | Black suits OLED TVs and dark rooms. |
| Night dimming | Off, 9 PM–6 AM, 10 PM–7 AM, 11 PM–7 AM, Always | Off | Darkens the home screen during those hours. Apps you open aren't affected. |
| Dimming strength | Light, Medium, Strong | Medium | How dark night dimming gets. |

## App tiles

| Setting | Options | Default | What it does |
|---|---|---|---|
| Tile size | Small, Medium, Large | Medium | |
| Tile artwork | Icon + name, App banner | Icon + name | App banner shows each TV app's own wide artwork; apps without one fall back to icon + name. |
| App names | Always, When highlighted, Never | Always | |
| Highlight zoom | Off, Subtle, Normal, Big | Normal | How much the highlighted tile grows. Off = no motion at all. |
| Highlight outline | Thin, Normal, Thick | Normal | Thick is easier to see from across the room. |
| Corners | Rounded, Soft, Square | Rounded | |
| Row position | Upper, Middle, Lower | Middle | Where the app row sits on screen. |
| "Edit apps" tile | On, Off | On | Hide the tile at the end of the row; edit apps from settings instead. |

## Clock & text

| Setting | Options | Default |
|---|---|---|
| Clock | On, Off | On |
| Clock format | Follow TV setting, 12-hour, 24-hour | Follow TV setting |
| Date | On, Off | Off |
| Greeting ("Good evening") | On, Off | Off |
| Season name | On, Off | Off |
| Tips line (under the apps) | On, Off | On |
| Text size | Normal, Large, Larger | Normal |
| Screen edge padding | None, Small, Medium, Large | None — use this if your TV crops the picture edges |

## Navigation & apps

| Setting | Options | Default | What it does |
|---|---|---|---|
| Wrap around | On, Off | Off | ▶ on the last tile jumps to the first (and ◀ on the first to the last). |
| Return to last app | On, Off | On | Coming back highlights the app you just used. Off: always the first app. |
| Home button on the home screen | Jump to first app, Stay where you are | Jump to first app | |
| Edit home screen apps | — | | Opens the app list (OK adds/removes). |
| Arrange home screen apps | — | | Vertical list: OK picks an app up, ▲ ▼ move it, OK puts it down. |
| Sort home screen apps A – Z | — | | One-off sort; press OK twice to confirm. |
| App list shows | All apps, TV apps only | All apps | TV apps only hides phone-style apps that may not work well with a remote. Apps already on home always show so they can be removed. |
| App list order | A – Z, TV apps first | A – Z | |

## Home button & remote

| Setting | What it does |
|---|---|
| Home button setup | Shows whether the Home-button service is on and how to turn it on (on the TV, or with two ADB commands). See [FEASIBILITY.md §3](FEASIBILITY.md) for why Google TV needs this. |
| Home button takeover | On/Off. Lets Home work the stock way again without turning the service off. |
| Extra Home button | A second remote button that also goes home — handy for remotes that send something other than HOME, or a spare button. Choose it in Remote button test; OK here clears it. |
| Remote button test | Shows the name and code of every button you press. With the service on it also shows Home and other buttons apps normally never see. Nothing is recorded outside this screen. |
| Open Android TV settings | The system's own settings (network, display, accounts…), useful when the stock home screen's gear is out of reach. |

## Lock

| Setting | What it does |
|---|---|
| Lock home screen layout | Hides "Edit apps" and the hold-OK menu so apps can't be added, removed or moved by accident. Apps still open normally; editing is still possible from settings. |
| Settings PIN | A 4-digit PIN asked for before settings open. Stored only as a salted hash. Number buttons on the remote work on the PIN pad. It keeps settings out of casual reach; it isn't strong security (anyone with ADB can clear the app's data). Forgotten PIN: see below. |

## Reset & about

- **Reset home screen apps** — back to the defaults that are installed (YouTube, Crunchyroll, Spotify, HBO Max). OK twice.
- **Reset all settings** — everything above back to default, except your apps and your PIN. OK twice.
- **About** — version, and the privacy promise: no ads, no tracking, no internet access.

## Forgotten PIN

Clearing the launcher's data removes the PIN, your settings and your app order (the apps themselves are untouched):

```bash
adb shell pm clear com.northeast.launcher          # release build
adb shell pm clear com.northeast.launcher.debug    # debug build
```
