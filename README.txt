--- How to use 2048 3x3 Trainer by Baactiba ---

1. Download the new .exe (Windows) or .jar (Linux) files from https://drive.google.com/drive/folders/1u37KFfSsL1hwQuKKZ_NHDN9eTB2tFW3_?usp=drive_link
2. Open GeneralizedTablegen and build your selected tables (more info on this later, or just DM me (baactiba on discord) for now if you can't figure it out).
3. Make sure the tables folder you just made is named "Tables" and is in another folder named "Tables." This outer folder must be in the same directory as the trainer exe or jar.
4. Make sure you have the newest version of Java installed.
5. Open the Trainer and enjoy (Windows)! If you use Linux, then run the .jar. Dunno how to do that on Linux so good luck. 



Game Controls:

Arrow Keys - Make moves.

W,A,S,D - Maybe make moves? It used to work but I might have broken it.

[Enter] - Let the tables play from current position.

K - Instantly kills game. Use this if you meet your table's goal, or the trainer may falsely think your accuracy is garbage.

R - Reset.

* If tables are playing, you will need to wait for the game to conclude or use Task Manager to close the program. Or do it some other way, but the close button probably won't work.

Game Review Controls:

Q - Toggle Game Review on. (must be in death screen)

R - Exit Game Review. (resets to new board, refreshes start position, and settings)

Left arrow/Right arrow - Look at next/previous mistake.



Using startpos.txt: 

To play from a position, convert the position to a string, similar to Storborg notation except from the bottom left to right, then upwards. 1024 becomes colon (:), not a. For example, https://2048verse.com/p/12a246033 would become :63243120
Then, you can run Trainer or just hit R if you already have it open.

To review a game from verse, paste the replay into startpos.txt and hit R. Then, hit space to run the game. Then, you can game review normally.

You can have multiple lines in startpos.txt. Look at the provided startpos.txt, it has many start positions that I've found useful and descriptions. To select one, just make sure that line is moved to the top and you save the file. Then you can hit R and the new startpos will be loaded. You can also put in a replay while keeping other lines as normal start positions.

Note: If the trainer freezes, then close the trainer (if it won't close, use task manager.) Then, clear mistakes.txt.



Using settings.txt:

Change the values to what you want them to be and then save and then hit R.

TableAutomoveWait: The amount of milliseconds between table automoves.

SpawnAlgorithm: spawn algorithm.
Algorithm 0 (Normal): What you're used to.
Algorithm 1 (Learny): Gives spawns according to a successful run's chance of having each spawn. Cannot force the player into a 0%.
Algorithm 2 (BigBrother): Gives you the worst spawn that isn't a 0%.
Algorithm 3 (TwosOnly): Destroys the universe. Wtf did you think?




Changelog:

GeneralizedTablegen 1.1 - Generalized Tablegen should now work on Linux.
GeneralizedTablegen 1.101 - Fixed custom tileset not working if the tileset contains 10 (1k).
GeneralizedTablegen 1.2 - Massively increased speed, the ability to use multiple threads, fixed bugs

Trainer 1.1 - Moves more accurate than 1 - (1e-14) are now considered perfect. Fixed moves made past 1k+512 not counting towards streak and accuracy.
Trainer 1.101 - Dramatically decreased memory usage.
