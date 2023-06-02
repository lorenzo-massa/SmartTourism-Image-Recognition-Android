import numpy as np
import pandas as pd
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
import glob
import sqlite3
import progressbar
import os
import argparse


def createDTVectors():
    #LOADING TEXT FILES
    path = "../models/src/main/assets/guides/*/English/guide.md"
    textPaths = glob.glob(path)
    print("Path used for guides: " + path + '\n' + "Number of files found: " + str(len(textPaths)))
    if(len(textPaths)==0):
        print("No files found in " + path + '\n' + "Please check the path and try again")
        exit()
    textList = []

    #REMOVING TEMPLATE MONUMENT FROM LIST
    for i in range(len(textPaths)):
        pathSplitted = textPaths[i].split(os.path.sep)[-3].split(' ')
        m = ' '.join([str(elem) for elem in pathSplitted])

        if (m == "Template Monument"):
            textPaths.pop(i)
            break

    #READING TEXT FILES
    for i, path in enumerate(textPaths):
        
        #open text file in read mode
        file = open(textPaths[i], "r")
        
        #read whole file to a string
        text_file = file.read()
        textList.append(text_file)
        
        #close file
        file.close()

    #CREATING DOC2VEC MODEL
    documents = [TaggedDocument(doc, [i]) for i, doc in enumerate(textList)]
    model = Doc2Vec(documents, vector_size=100, window=5, min_count=1, workers=4, epochs=20)
    desc_vectors = np.zeros((len(textList), model.vector_size))
    for i in range(len(textList)):
        desc_vectors[i] = model.dv[i]


    #CREATING SQL LITE DATABASE

    con = sqlite3.connect("../models/src/main/assets/databases/doc2vec_db.sqlite")
    cur = con.cursor()

    cur.execute("DROP TABLE IF EXISTS docToVec")

    cur.execute(""" CREATE TABLE docToVec (monument, vec) """)

    widgets = [
        "Saving database ... ", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(maxval=len(desc_vectors), widgets=widgets).start()

    for i, v in enumerate(desc_vectors):
        # Insert a row of data
        val = str(v)
        pathSplitted = textPaths[i].split(os.path.sep)[-3].split(' ')
        m = ' '.join([str(elem) for elem in pathSplitted])

        sql = ''' INSERT INTO docToVec (monument,vec)
                VALUES(?,?) '''
        new = cur.execute(sql, (m,val))

        # Save (commit) the changes
        con.commit()
        pbar.update(i)

    con.close()
    pbar.finish()

    print("DB Saved in " + os.path.realpath('../models/src/main/assets/databases'))