# MonitorFileAndDirectory
Java command line tool to monitor the size of a file or directory.
- First argument is the full path, checked with regex "((?:[^\/]*\/)*)(.*)"
- Usage: First argument should be the path. Without quotes. Without spaces. Can be file (.*) or directory (no ending).
- Listens for termination signal and gracefully shut downs threads
- Uses real file/directory size according to the file system
- Checks every second (you might want to switch this to a "calulcation-only-when-changes-occured")
- Catches possible exceptions, states clear error and usage messages
