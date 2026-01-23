Source shared_.tape

# Setup
Hide
Type jbang input-form-demo
Enter
Sleep 2
Show

# Recording
Sleep 1s
Screenshot screenshots/input-form-demo.svg
# Type in first name
Type "John"
Sleep 0.5
# Tab to last name
Tab
Sleep 0.5
# Type in last name
Type "Doe"
Sleep 0.5
# Tab to age
Tab
Sleep 0.5
# Type age
Type "30"
Sleep 0.5
# Tab back to first name
Tab
Tab
Sleep 0.5
# Use Ctrl+A to move to start
Type "\x01"
Sleep 0.3
# Type prefix
Type "Mr. "
Sleep 0.5
# Tab to age
Tab
Tab
Sleep 0.5
# Use j/k to adjust age
Type "k"
Sleep 0.3
Type "k"
Sleep 0.3
Type "j"
Sleep 0.5
# Submit form
Enter
Sleep 1s

