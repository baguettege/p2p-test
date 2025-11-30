## p2p testing in java 25

v1.4

small peer-to-peer program

all saved data is stored in Documents/p2p-test

authentication keys are produced when on first run of the program, or when you run the command 'key reset'

do not share your private key file, located in Documents/p2p-test/keys/private.key, if you somehow do run the command 'key reset'

in order to connect to a peer, you must trust their public key via running the command 'key add'

and selecting their public.key file, that they have sent you (found in Documents/p2p-test/keys/public.key)

## commands

````
main console:

- cmd
    lists all available commands

- connect [ip:port]
    attempts to connect to a peer with the given ip:port

- port open [port number]
    opens a ServerSocket which allows for other peers to make connects to you
    
- port close
    closes the open port by closing the ServerSocket, disabling inbound connections
    
- key add
    allows you to select a peer's public .key file to trust connections to
    
- key remove
    allows you to remove a .key file from your trusted public keys
    
- key reset
    resets your own private and public keys
    will require you to re-share your public key so that peers can trust it again
    
- exit
    closes the program
    
- debug key [path to public key]|[path to private key]
    lets you set the public and private keys used for authentication, is mainly for testing
    e.g. debug key C:\Users\user1\Downloads\public1.key|C:\Users\user1\Downloads\private1.key
````

````
peer consoles:

- cmd
    lists all available commands
    
- msg [text]
    send a message to the peer
    
- ping
    ping the peer, they will echo back, will print the latency (time taken) to echo
    
- file upload
    select a file to send to the peer, they will have to accept this file transfer
    
- file accept
    accept a file transfer request sent by the peer
    
- file decline
    decline a file transfer request sent by the peer
    
- file cancel upload
    cancels uploading a file to the peer
    
- file cancel download
    cancels downloading a file to the peer
    
- disconnect
    disconnects from the peer
````