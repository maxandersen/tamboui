Source shared_.tape

# Setup
Hide
Type "jbang thread-dump-review-demo"
Enter
Sleep 2
Show

# Recording
Sleep 2
Screenshot screenshots/thread-dump-review-demo.svg

# Navigate snapshots
Down
Sleep 1

# Move to threads and select another thread
Right
Down
Sleep 1

# Mark baseline and open comparison tab
Type "b"
Sleep 1
Type "2"
Sleep 2

# Explore lock graph and deadlock tabs
Type "3"
Sleep 2
Type "4"
Sleep 2

# Move into detail pane and scroll comparison content
Type "2"
Sleep 1
Right
Down 4
Sleep 2

# Return to thread detail tab
Type "1"
Sleep 2

# Search by text
Type "/worker"
Enter
Sleep 2

# Toggle regex mode and search with pattern
Ctrl+r
Type "/http-.*"
Enter
Sleep 2

# Quit
Type "q"
