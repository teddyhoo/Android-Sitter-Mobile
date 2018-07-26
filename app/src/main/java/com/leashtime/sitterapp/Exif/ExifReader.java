package com.leashtime.sitterapp.Exif;

import java.io.IOException;
import java.io.InputStream;

class ExifReader {
    private final ExifInterface mInterface;
    ExifReader(ExifInterface iRef) {
        mInterface = iRef;
    }
    /**

     */
    protected ExifData read(InputStream inputStream) throws ExifInvalidFormatException,
            IOException {
        ExifParser parser = ExifParser.parse(inputStream, mInterface);
        ExifData exifData = new ExifData(parser.getByteOrder());
        ExifTag tag = null;
        int event = parser.next();
        while (event != ExifParser.EVENT_END) {
            switch (event) {
                case ExifParser.EVENT_START_OF_IFD:
                    exifData.addIfdData(new IfdData(parser.getCurrentIfd()));
                    break;
                case ExifParser.EVENT_NEW_TAG:
                    tag = parser.getTag();
                    if (!tag.hasValue()) {
                        parser.registerForTagValue(tag);
                    } else {
                        exifData.getIfdData(tag.getIfd()).setTag(tag);
                    }
                    break;
                case ExifParser.EVENT_VALUE_OF_REGISTERED_TAG:
                    tag = parser.getTag();
                    if (tag.getDataType() == ExifTag.TYPE_UNDEFINED) {
                        parser.readFullTagValue(tag);
                    }
                    exifData.getIfdData(tag.getIfd()).setTag(tag);
                    break;
                case ExifParser.EVENT_COMPRESSED_IMAGE:
                    byte buf[] = new byte[parser.getCompressedImageSize()];
                    if (buf.length == parser.read(buf)) {
                        exifData.setCompressedThumbnail(buf);
                    } else {
                    }
                    break;
                case ExifParser.EVENT_UNCOMPRESSED_STRIP:
                    buf = new byte[parser.getStripSize()];
                    if (buf.length == parser.read(buf)) {
                        exifData.setStripBytes(parser.getStripIndex(), buf);
                    } else {
                    }
                    break;
            }
            event = parser.next();
        }
        return exifData;
    }
}
