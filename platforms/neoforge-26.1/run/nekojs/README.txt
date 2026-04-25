=== NekoJS Script Directory Guide ===
- startup_scripts: Loaded during game startup. Used for registering items and blocks. Changes require a full game restart.
- server_scripts: Executed when the world/server loads. Used for recipes and event handling. Can be reloaded with /reload.
- client_scripts: Runs on the client only. Used for GUI, key bindings, etc.
- Note: Automatically generated type declaration files (.d.ts) are located in the .probe folder in the game root directory. Do not modify them manually.