--- How to use 2048 3x3 Trainer by Baactiba ---

1. You must first download the Tables folder from https://mega.nz/folder/BHFCEJiQ#y2oUfHt-VpQRJLxX7iOrbg and place it in the same folder as the folder this readme is located in.
2. Make sure you have the newest version of Java installed.
3. Open command prompt.
4. Copy the location of the Trainer folder. It should look something like this: C:\Users\YourUsername\OneDrive\Desktop\Trainer
5. Type "cd " and then paste.
6. Type "javac Trainer.java"

Then, you can use "java Trainer" to run. Make sure that you cd to the Trainer folder every time you open command prompt!


Game Controls:

Arrow Keys - Make moves.
W,A,S,D - Maybe make moves? It used to work but I might have broken it.
[Enter] - Let the tables play from position. If you want to adjust the speed, change the number of milliseconds on line 1111.
[Shift] - Allow my AI to play from position. If you want to configure the thinking time, change the number of milliseconds on line 62.

* If tables or AI are playing, you will need to wait for the game to conclude or use Task Manager to close the program. Or do it some other way, but the close button probably won't work.

Game Review Controls:

Q - Toggle Game Review on. (must be in death screen)

R - Exit Game Review. (resets to new board)

Left arrow/Right arrow - Look at next/previous mistake.

Using startpos.txt: 

To play from a position, convert the position to a string, similar to Storborg notation except from the bottom left to right, then upwards. 1024 becomes colon (:), not a. For example, https://2048verse.com/p/12a246033 would become :63243120
Then, you can run Trainer or just reset if you already have it open.

To review a game from verse, paste the replay into startpos.txt. Restart the trainer. Then, hit space to run the game. Then, you can game review normally. You cannot continue playing normally after this, you must close the program.

To play from score, leave startpos.txt blank, and then after resetting a game, the border will no longer change colors as feedback.


Note: If the trainer freezes, then close the trainer (if it won't close, use task manager.) Then, clear mistakes.txt.

