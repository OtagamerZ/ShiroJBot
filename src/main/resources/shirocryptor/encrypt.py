# -*- coding: utf-8 -*-
from cryptography.fernet import Fernet
import hashlib
import os.path
import sys

def generate_key():
    key = Fernet.generate_key()
    open("key.sbk", "wb").write(key)
    print("Nova chave de criptografia gerada - {0}".format(key.decode("UTF-8")))
    return key

def encrypt(target, key):
    f = Fernet(key)
    fb = open(target, "rb").read()
    return f.encrypt(fb)


if len(sys.argv) < 2:
    print("É necessário informar o arquivo para ser criptografado")
    exit()
    
target = sys.argv[1]
key = ""
if os.path.exists("key.sbk"):
    key = open("key.sbk", "r").read()
else:
    key = generate_key()

if not os.path.exists(target):
    print("Arquivo não encontrado, verifique se digitou o nome corretamente")
    exit()

content = encrypt(target, key)

open("{0}.sbd".format(target), "wb").write(content)
open("{0}.hash".format(target), "w").write(hashlib.md5(content).hexdigest())

print("Arquivo encriptado com sucesso, arquivo contendo a chave MD5 foi criada junto ao arquivo criptografado")