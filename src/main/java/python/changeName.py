import os
import re
import sys


def allDir(root, fileList):
    print(fileList)
    for fileName in fileList:
        cur = os.path.join(root, fileName)
        print("判断的路径：" + cur)
        if os.path.isdir(cur):
            fileList = os.listdir(cur)
            allDir(cur, fileList)
        else:
            modify(root, fileName)


def modify(root, fileName):
    # 匹配文件名正则表达式
    cur = os.path.join(root, fileName)
    pat = re.match(".+\.(S.+?)\..+", fileName)
    # 进行匹配
    if pat:
        str = "How I Met Your Mother " + pat.group(1)+".mkv"
        print("修改文件:" + fileName)
        # 文件重新命名
        os.rename(cur, os.path.join(root, str))


fileList = os.listdir(r".")
root = os.getcwd()
allDir(root, fileList)
# 遍历文件夹中所有文件
print("***************************************")
# 刷新
sys.stdin.flush()
