# -*- coding: utf-8 -*-
from cryptography.fernet import Fernet
import hashlib
import os.path
import sys

def decrypt(target, key):
    f = Fernet(key)
    fb = open(target, "rb").read()
    return f.decrypt(fb)

if not os.path.exists(target):
    print("Arquivo não encontrado, verifique se digitou o nome corretamente")
    exit()
elif not target.endswith(".sbd"):
    print("Arquivo inválido, verifique se digitou o nome do arquivo completo, incluido a extensão (ex: arquivo.txt.sbd)")
    exit()

if os.path.exists(target.replace("sbd", "hash")):
    hashKey = open(target.replace("sbd", "hash")).read()
    if hashKey == hashlib.md5(open(target, "rb").read()).hexdigest():
        print("Chave MD5 validado com sucesso, o arquivo equivale ao original")
    else:
        print("Chave MD5 não equivale a original, por favor requisite o reenvio do arquivo e/ou chave")
        exit()
else:
    print("Chave MD5 não encontrada, por favor requisite o reenvio da chave")
    exit()


try:
    content = decrypt(target, key)

    open(target.replace(".sbd", ""), "wb").write(content)
    print("Arquivo decriptado com sucesso")
except:
    print("Chave de criptografia incorreta, por favor requisite o reenvio da chave")