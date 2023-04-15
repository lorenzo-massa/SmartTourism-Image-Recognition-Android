# necessary packages
from configparser import Interpolation

import cv2
import imutils
import tensorflow as tf
import numpy as np



def printIMG(image):
    # show the image, provide window name first
    cv2.imshow('image window', image)
    # add wait key. window waits until user presses a key
    cv2.waitKey(0)
    # and finally destroy/close all open windows
    cv2.destroyAllWindows()

class AspectAwarePreprocessor:
    def __init__(self, width, height, inter=cv2.INTER_LINEAR):
        # store the target image width, height, and interpolation
        self.width = width
        self.height = height
        self.inter = inter

    def preprocess(self, image):
        # grab the dimensions of the image and then initialize
        # the deltas to use when cropping

        (h, w) = image.shape[:2]

        # crop
        #if w < h:
        #    image = imutils.resize(image, width=self.width, inter=self.inter) #tiene aspect ratio
        #    dH = int((image.shape[0] - self.height) / 2.0)
        #else:
        #    image = imutils.resize(image, height=self.height, inter=self.inter)
        #    dW = int((image.shape[1] - self.width) / 2.0)
 

        #center crop
        cropSize = min(h,w)

        image=tf.image.resize_with_crop_or_pad(
            image, cropSize, cropSize
        )

        #image = imutils.resize(image, width=self.width, inter=self.inter)

        #(h, w) = image.shape[:2]
        #image = image[dH:h - dH, dW:w - dW]

        #resize bilinear
        image = tf.image.resize(image,[self.width, self.height],method='nearest').numpy().astype(np.uint8)

        #precisione android (se sotto .05 prendi numero inferiore)

        #image = cv2.resize(image, (self.width, self.height),
        #                  interpolation=self.inter) #non tiene aspect ratio e interpola (distorge img)
        
        return image

    def preprocessAugumentation(self,image,zoom1,zoom2):
        (h, w) = image.shape[:2]
        cropSize = min(h,w)

        imageNormal=tf.image.resize_with_crop_or_pad(
            image, cropSize, cropSize
        )
        imageNormal = tf.image.resize(imageNormal,[self.width, self.height],method='nearest').numpy().astype(np.uint8)

        imageZoom1=tf.image.resize_with_crop_or_pad(
            image, int(cropSize*zoom1), int(cropSize*zoom1)
        )
        imageZoom1 = tf.image.resize(imageZoom1,[self.width, self.height],method='nearest').numpy().astype(np.uint8)

        imageZoom2=tf.image.resize_with_crop_or_pad(
            image, int(cropSize*zoom2), int(cropSize*zoom2)
        )
        imageZoom2 = tf.image.resize(imageZoom2,[self.width, self.height],method='nearest').numpy().astype(np.uint8)

        return imageNormal,imageZoom1,imageZoom2
