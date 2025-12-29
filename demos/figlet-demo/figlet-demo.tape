Output "demos/figlet-demo/figlet-demo.gif"
Set Theme "Builtin Solarized Dark"
Set FontSize 18
Set Width 1200
Set Height 700

Type "./run-demo.sh figlet-demo"
Enter
Sleep 1200ms

# Cycle fonts
Type "j"
Sleep 250ms
Type "j"
Sleep 250ms
Type "k"
Sleep 400ms

# Focus input, change text, then go back to list and quit
Tab
Sleep 250ms
Ctrl+U
Sleep 200ms
Type "Hello Figlet"
Sleep 700ms
Tab
Sleep 250ms
Type "q"

