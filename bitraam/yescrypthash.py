# -*- coding: utf-8 -*-

import sys

try:
    from yescrypt_hash import getPoWHash
    import_success = True
    load_libyescrypthash = False
except ImportError:
    import_success = False
    load_libyescrypthash = True
    

if load_libyescrypthash:
    from ctypes import cdll, create_string_buffer, byref

    if sys.platform == 'darwin':
        name = 'libyescrypthash.dylib'
    elif sys.platform in ('windows', 'win32'):
        name = 'libyescrypthash-0.dll'
    else:
        name = 'libyescrypthash.so'

    try:
        lyescrypthash = cdll.LoadLibrary(name)
        yescrypt_hash = lyescrypthash.yescrypt_hash
    except:
        load_libyescrypthash = False
        
if load_libyescrypthash:
    hash_out = create_string_buffer(32)

    def getPoWHash(header):
        yescrypt_hash(header, byref(hash_out))
        return hash_out

if not import_success and not load_libyescrypthash:
    raise ImportError('Can not import yescrypt_hash')