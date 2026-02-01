#!/usr/bin/env python
import gzip
import json
from json import loads, dumps
import sys
from sys import exit, argv
import base64
import urllib.request, urllib.error, urllib.parse

if len(argv) < 3:
    print('Arguments: <rpc_username> <rpc_password> [<rpc_port>]')
    sys.exit(1)

# From electrum.
def bits_to_target(bits):
    bitsN = (bits >> 24) & 0xff
    if not (bitsN >= 0x03 and bitsN <= 0x1e):
        raise BaseException("First part of bits should be in [0x03, 0x1e]")
    bitsBase = bits & 0xffffff
    if not (bitsBase >= 0x8000 and bitsBase <= 0x7fffff):
        raise BaseException("Second part of bits should be in [0x8000, 0x7fffff]")
    return bitsBase << (8 * (bitsN-3))

def rpc(method, params):
    data = {
        "jsonrpc": "1.0",
        "id":"1",
        "method": method,
        "params": params
    }

    data_json = dumps(data)
    username = argv[1]
    password = argv[2]
    port = 4561
    if len(argv) > 3:
        port = argv[3]
    url = "http://127.0.0.1:{}/".format(port)
    req = urllib.request.Request(url, data_json.encode("utf-8"), {'content-type': 'application/json'})

    base64string = base64.encodebytes(('%s:%s' % (username, password)).encode()).decode().replace('\n', '')
    req.add_header("Authorization", "Basic %s" % base64string)

    response_stream = urllib.request.urlopen(req)
    json_response = response_stream.read()

    return loads(json_response)

# Electrum checkpoints are blocks 2015, 2015 + 2016, 2015 + 2016*2, ...
# python3.8  get_bitraam_checkpoint_array.py bitraam bitraam 31414
i = 2015
INTERVAL = 2016

checkpoints = []
hash_headers = []
block_count = int(rpc('getblockcount', [])['result'])
print(('Blocks: {}'.format(block_count)))
while True:
    h = rpc('getblockhash', [i])['result']
    block = rpc('getblock', [h])['result']
    
    for hh in range(i,i-25,-1):
     hh1 = rpc('getblockhash', [hh])['result']
     header1 = rpc('getblockheader', [hh1, False])['result']
     hash_headers.append([hh,
                        header1])

    checkpoints.append([
        block['hash'],
        bits_to_target(int(block['bits'], 16)),
        hash_headers
    ])
    hash_headers = []

    i += INTERVAL
    if i > block_count:
        print('Done.')
        break

#with open('checkpoints.json', 'w+') as f:
#    f.write(dumps(checkpoints, indent=4, separators=(',', ':')))

# File path for the output .json.gz file
file_path = "checkpoints.json.gz"

# Create and write to the .json.gz file
try:
    with gzip.open(file_path, 'wt', encoding='UTF-8') as zipfile:
        json.dump(checkpoints, zipfile, indent=4) # Using indent for readability
    print(f"Successfully created '{file_path}'")
except IOError as e:
    print(f"Error creating file: {e}")