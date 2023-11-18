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
from processing_monuments import createDB
from deep_augumentation import augment

np.set_printoptions(threshold=np.inf)

types = [  # neural networks
    ('MobileNetV3_Large_100', 'models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_100_224_classification_5_default_1.tflite'),
    ('MobileNetV3_Large_075', 'models/src/main/assets/lite-model_imagenet_mobilenet_v3_large_075_224_classification_5_default_1.tflite'),
    ('MobileNetV3_Small_100', 'models/src/main/assets/lite-model_imagenet_mobilenet_v3_small_100_224_classification_5_default_1.tflite')
]

# Set ALL_DATASET to True if you want to use all the images of the dataset to train the model,
# set to False if you want to split the dataset in train_set and test_set
ALL_DATASET = True

ap = argparse.ArgumentParser()

ap.add_argument('-i', '--images', help='path of dataset images')

# add optional argument to skip the creation of the test_set (default False)
ap.add_argument('-f', '--fast', help='skip the creation of the features dataset', action='store_true')

if ap.parse_args().fast:
    print("\n\n[INFO]: Skipping the creation of the features dataset")
    createDB()
    exit()

args = vars(ap.parse_args())

# LOAD IMAGE PATHS
image_directories = [d for d in os.listdir(args['images']) if os.path.isdir(os.path.join(args['images'], d))]

dataImages = []
for directory in image_directories:
    print(f'Processing image directory: {directory}')
    image_paths_jpg = glob.glob(os.path.join(args['images'], directory, '*.jpg'))
    image_paths_jpeg = glob.glob(os.path.join(args['images'], directory, '*.jpeg'))
    image_paths_png = glob.glob(os.path.join(args['images'], directory, '*.png'))
    dataImages.extend(image_paths_jpg + image_paths_jpeg + image_paths_png)


dataset = list()

# GENERATING SETS
if not ALL_DATASET:
    train_set, test_set = train_test_split(
        dataImages, test_size=0.33, random_state=1331, shuffle=True
    )
    dataImages = train_set

# progress bar
widgets = [
    "Building dataset ...", progressbar.Percentage(), " ",
    progressbar.Bar(), " ", progressbar.ETA()
]
pbar = progressbar.ProgressBar(
    maxval=len(dataImages), widgets=widgets
).start()

if len(dataImages) == 0:
    print("\n\n[ERROR]: No images found in the specified path: " + args['images'] + "\n\n")
    exit()

monuments = dict()

for (i, path) in enumerate(dataImages):
    if path.split(os.path.sep)[-2].count('_') > 0:
        pathSplitted = path.split(os.path.sep)[-2].split('_')
    else:
        pathSplitted = path.split(os.path.sep)[-2].split(' ')
    monument = pathSplitted[0] + " " + pathSplitted[1]
    tupleImage = (monument, path)
    dataset.append(tupleImage)
    if monument not in monuments.keys():
        monuments[monument] = 1
    else:
        monuments[monument] += 1
    pbar.update(i)
pbar.finish()

IMAGE_PER_MONUMENT = 20

# for each monument with less than 20 images, augment the dataset with the images of the same monument calling de function augment_images
for monument in monuments.keys():
    if monuments[monument] < IMAGE_PER_MONUMENT:
        print("\n\n[INFO]: Augmenting " + str(IMAGE_PER_MONUMENT - monuments[monument]) + " images of " + monument + " ..." )
        # Get n random images of the monument
        images = [path for (m, path) in dataset if m == monument and "_aug" not in path]
        images = np.random.choice(images, size=(IMAGE_PER_MONUMENT - monuments[monument]))
        i = 1
        for image in images:
            newpath = augment(image, i)
            tupleImage = (monument, newpath)
            dataset.append(tupleImage)
            i += 1

# BUILD FEATURES

iap = ImageToArrayPreprocessor()
aap = AspectAwarePreprocessor(224, 224)

# loop over images
for dType, modelPath in types:
    print('[INFO]: Working with {} ...'.format(dType))
    extractor = Extractor(dType, path=modelPath)
    db = []
    widgets = [
        "Extracting features ... ", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(maxval=len(dataset), widgets=widgets).start()

    index = 0

    for monument, path in dataset:
        # preprocessing
        image = cv2.imread(path)
        if np.shape(image) == ():  # latest numpy / py3
            print("\n\n[ERROR]: Image not found: " + path)
            continue  # fail !!
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        h = image.shape[0]
        w = image.shape[1]

        # AUGUMENTATION 3X3

        imgheight = h - (h % 3)
        imgwidth = w - (w % 3)

        y1 = 0
        M = imgheight // 3
        N = imgwidth // 3

        tiles = list()

        for y in range(0, imgheight, M):
            for x in range(0, imgwidth, N):
                y1 = y + M
                x1 = x + N
                tiles.append(image[y:y + M, x:x + N])

        # PREPROCESS IMG WITH AUGUMENTATION ZOOM

        image, image1, image2 = aap.preprocessAugumentation(image, 0.5, 0.3)
        # toArray preprocess
        image = iap.preprocess(image)
        image1 = iap.preprocess(image1)
        image2 = iap.preprocess(image2)

        features = extractor.extract(image)
        features1 = extractor.extract(image1)
        features2 = extractor.extract(image2)

        # SAVING
        if isinstance(features, np.ndarray):
            db.append([features, monument])
        if isinstance(features1, np.ndarray):
            db.append([features1, monument])
        if isinstance(features2, np.ndarray):
            db.append([features2, monument])

        # PREPROCESS TILES FOR AUGUMENTATION 3x3

        for t in tiles:
            t = aap.preprocess(t)

            # toArray preprocess
            t = iap.preprocess(t)
            featuresTile = extractor.extract(t)

            # SAVING
            if isinstance(featuresTile, np.ndarray):
                db.append([featuresTile, monument])

        index += 1
        pbar.update(index)
    pbar.finish()

    # CREATING NEW SQL LITE DATABASE

    # Delete old database
    if os.path.exists("models/src/main/assets/databases/" + dType + "_db.sqlite"):
        os.remove("models/src/main/assets/databases/" + dType + "_db.sqlite")

    # Create new database
    con = sqlite3.connect("models/src/main/assets/databases/" + dType + "_db.sqlite")
    cur = con.cursor()


    cur.execute("DROP TABLE IF EXISTS monuments")

    cur.execute(""" CREATE TABLE monuments (monument, features) """)

    widgets = [
        "Saving database ... ", progressbar.Percentage(), " ",
        progressbar.Bar(), " ", progressbar.ETA()
    ]
    pbar = progressbar.ProgressBar(maxval=len(db), widgets=widgets).start()

    for i, (matrix, m) in enumerate(db):
        # Insert a row of data
        val = str(matrix)

        sql = ''' INSERT INTO monuments (monument,features)
                VALUES(?,?) '''
        new = cur.execute(sql, (m, val))

        # Save (commit) the changes
        con.commit()
        pbar.update(i)

    con.close()
    pbar.finish()

# print("\n\nDB Saved in " + os.path.realpath('../models/src/main/assets/databases'))

print("\n\n[INFO]: Processing monuments")
createDB()
print("\n\n[INFO]: Script terminated correctly")
