p2p-test

p2p testing in java

all saved data is stored in Documents/p2p-test

v1 - basic connections, message sharing
v1.1 - verification, authentication, commenting, additional packets
v1.2 - file sending
v1.3 - improved file sending
v1.35 - accept/decline file transfer, very spaghetti code

in v1.2, file sending is primitive:
- it loads the entire file into ram at once
- so can crash the program if the file is too large
- there is no accept/declining file sending, can be unsafe if connected to untrusted peer

in v1.3, improved file sending:
- data is sent in small packets as a stream instead of all at once, should prevent crashing
- no accept/declining file sending in place yet

in v1.35:
- each file transfer has to be accepted/declined
- no automatic cooldown
- code is very spaghetti
- working on cleaning it up