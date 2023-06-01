# -*- coding: utf-8 -*-
import glob
import argparse
from preprocessors.aspectawarepreprocessor import printIMG
import progressbar
import numpy as np
from scripts import Extractor
from preprocessors import AspectAwarePreprocessor
from preprocessors import ImageToArrayPreprocessor
import cv2
import os
import sqlite3
import pickle
from sklearn.model_selection import train_test_split
from gensim.models.doc2vec import Doc2Vec, TaggedDocument
import glob
from processing_docToVec import createDTVectors

np.set_printoptions(threshold=np.inf)

types = [ #neural networks
    ('MobileNetV3_Large_100', '../models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_100_224_classification_5_default_1.tflite'), 
    ('MobileNetV3_Large_075', '../models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_075_224_classification_5_default_1.tflite'),
    ('MobileNetV3_Small_100', '../models/src/main/assets/lite-model_imagenet_mobilenet_v3_small_100_224_classification_5_default_1.tflite')
]

#Set ALL_DATASET True if you want to use all the images of the dataset to train the model, False if you want to split the datset in train_set and test_set
ALL_DATASET = False

ap = argparse.ArgumentParser()
ap.add_argument('-i', '--images', help='path of datset images')
args = vars(ap.parse_args())

# LOAD IMAGE PATHS
dataImages = glob.glob(args['images']+"*/*/*.jpg")
dataset = list()

# GENERATING SETS
if not ALL_DATASET:
    train_set, test_set = train_test_split(
        dataImages, test_size=0.33, random_state=1331, shuffle=True
    )
    dataImages=train_set

# progress bar
widgets = [
    "Building dataset ...", progressbar.Percentage(), " ",
    progressbar.Bar(), " ", progressbar.ETA()
]
pbar = progressbar.ProgressBar(
    maxval=len(dataImages), widgets=widgets
).start()


for (i, path) in enumerate(dataImages):
    pathSplitted = path.split(os.path.sep)[-2].split('_')
    monument = pathSplitted[0]+" "+pathSplitted[1]
    tupleImage = (monument,path)
    dataset.append(tupleImage)
    pbar.update(i)
pbar.finish()


#BUILD FEATURES

iap = ImageToArrayPreprocessor()
aap = AspectAwarePreprocessor(224, 224)


# loop over images
for dType,modelPath in types:
    print('[INFO]: Working with {} ...'.format(dType))
    extractor = Extractor(dType,path=modelPath)
    db = []
    widgets = [
        "Extracting features ... ", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(maxval=len(dataset), widgets=widgets).start()

    index = 0

    for monument,path in dataset:
        # preprocessing
        image = cv2.imread(path)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        h = image.shape[0]
        w = image.shape[1]

        #AUGUMENTATION 3X3

        imgheight=  h - (h%3)
        imgwidth= w - (w%3)

        y1 = 0
        M = imgheight//3
        N = imgwidth//3

        tiles = list()

        for y in range(0,imgheight,M):
            for x in range(0, imgwidth, N):
                y1 = y + M
                x1 = x + N
                tiles.append(image[y:y+M,x:x+N])


        #PREPROCESS IMG WITH AUGUMENTATION ZOOM

        image,image1,image2 = aap.preprocessAugumentation(image,0.5,0.3)
        #toArray preprocess
        image = iap.preprocess(image)
        image1 = iap.preprocess(image1)
        image2 = iap.preprocess(image2)


        features = extractor.extract(image)
        features1 = extractor.extract(image1)
        features2 = extractor.extract(image2)
    
        #SAVING
        if isinstance(features, np.ndarray):
            db.append([features, monument])
        if isinstance(features1, np.ndarray):
            db.append([features1, monument])
        if isinstance(features2, np.ndarray):
            db.append([features2, monument])
        
        #PREPROCESS TILES FOR AUGUMENTATION 3x3

        for t in tiles:
            t = aap.preprocess(t)

            #toArray preprocess
            t = iap.preprocess(t)
            featuresTile = extractor.extract(t)

            #SAVING
            if isinstance(featuresTile, np.ndarray):
                db.append([featuresTile, monument])
            

        index += 1
        pbar.update(index)
    pbar.finish()

    #SAVING DB IN PKG FOR PYTHON TESTS

    with open('./features/'+ dType + '_features.pck', 'wb') as fp:
        pickle.dump(db, fp)

    #SAVING TEST_SET IF REQUIRED

    if not ALL_DATASET:
        with open('./features/test_set.pck', 'wb') as fp:
            pickle.dump(test_set, fp)

    
    #CREATING SQL LITE DATABASE

    con = sqlite3.connect("../models/src/main/assets/databases/"+dType+"_db.sqlite")
    cur = con.cursor()

    cur.execute("DROP TABLE IF EXISTS monuments")

    cur.execute(""" CREATE TABLE monuments (monument, features) """)

    widgets = [
        "Saving database ... ", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(maxval=len(db), widgets=widgets).start()

    for i, (matrix,m) in enumerate(db):
        # Insert a row of data
        val = str(matrix)

        sql = ''' INSERT INTO monuments (monument,features)
                VALUES(?,?) '''
        new = cur.execute(sql, (m,val))

        # Save (commit) the changes
        con.commit()
        pbar.update(i)

    con.close()
    pbar.finish()

print("DB Saved in " + os.path.realpath('../models/src/main/assets/databases'))

print("Creating Doc2Vec vectors")
createDTVectors()
print("Doc2Vec vectors created successfully")



