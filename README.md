p2p-test

p2p testing in java

all saved data is stored in Documents/p2p-data

v1 - basic connections, message sharing
v1.1 - verification, authentication, commenting, additional packets
v1.2 - file sending

in v1.2, file sending is primitive:
- it loads the entire file into ram at once
- so can crash the program if the file is too large
- there is no accept/declining file sending, can be unsafe if connected to untrusted peer