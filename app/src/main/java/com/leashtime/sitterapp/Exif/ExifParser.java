package com.leashtime.sitterapp.Exif;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

public class ExifParser {
    private static final boolean LOGV = false;
    public static final int EVENT_START_OF_IFD = 0;
    public static final int EVENT_NEW_TAG = 1;
    public static final int EVENT_VALUE_OF_REGISTERED_TAG = 2;
    public static final int EVENT_COMPRESSED_IMAGE = 3;
    public static final int EVENT_UNCOMPRESSED_STRIP = 4;
    public static final int EVENT_END = 5;
    public static final int OPTION_IFD_0 = 1 << 0;
    public static final int OPTION_IFD_1 = 1 << 1;
    public static final int OPTION_IFD_EXIF = 1 << 2;
    public static final int OPTION_IFD_GPS = 1 << 3;
    public static final int OPTION_IFD_INTEROPERABILITY = 1 << 4;
    public static final int OPTION_THUMBNAIL = 1 << 5;
    protected static final int EXIF_HEADER = 0x45786966; // EXIF header "Exif"
    protected static final short EXIF_HEADER_TAIL = (short) 0x0000; // EXIF header in APP1
    // TIFF header
    protected static final short LITTLE_ENDIAN_TAG = (short) 0x4949; // "II"
    protected static final short BIG_ENDIAN_TAG = (short) 0x4d4d; // "MM"
    protected static final short TIFF_HEADER_TAIL = 0x002A;
    protected static final int TAG_SIZE = 12;
    protected static final int OFFSET_SIZE = 2;
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    protected static final int DEFAULT_IFD0_OFFSET = 8;
    private final CountedDataInputStream mTiffStream;
    private final int mOptions;
    private int mIfdStartOffset;
    private int mNumOfTagInIfd;
    private int mIfdType;
    private ExifTag mTag;
    private ExifParser.ImageEvent mImageEvent;
    private int mStripCount;
    private ExifTag mStripSizeTag;
    private ExifTag mJpegSizeTag;
    private boolean mNeedToParseOffsetsInCurrentIfd;
    private final boolean mContainExifData;
    private int mApp1End;
    private int mOffsetToApp1EndFromSOF;
    private byte[] mDataAboveIfd0;
    private int mIfd0Position;
    private int mTiffStartPosition;
    private final ExifInterface mInterface;
    private static final short TAG_EXIF_IFD = ExifInterface
            .getTrueTagKey(ExifInterface.TAG_EXIF_IFD);
    private static final short TAG_GPS_IFD = ExifInterface.getTrueTagKey(ExifInterface.TAG_GPS_IFD);
    private static final short TAG_INTEROPERABILITY_IFD = ExifInterface
            .getTrueTagKey(ExifInterface.TAG_INTEROPERABILITY_IFD);
    private static final short TAG_JPEG_INTERCHANGE_FORMAT = ExifInterface
            .getTrueTagKey(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT);
    private static final short TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = ExifInterface
            .getTrueTagKey(ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
    private static final short TAG_STRIP_OFFSETS = ExifInterface
            .getTrueTagKey(ExifInterface.TAG_STRIP_OFFSETS);
    private static final short TAG_STRIP_BYTE_COUNTS = ExifInterface
            .getTrueTagKey(ExifInterface.TAG_STRIP_BYTE_COUNTS);
    private final TreeMap<Integer, Object> mCorrespondingEvent = new TreeMap<Integer, Object>();
    private boolean isIfdRequested(int ifdType) {
        switch (ifdType) {
            case IfdId.TYPE_IFD_0:
                return (mOptions & OPTION_IFD_0) != 0;
            case IfdId.TYPE_IFD_1:
                return (mOptions & OPTION_IFD_1) != 0;
            case IfdId.TYPE_IFD_EXIF:
                return (mOptions & OPTION_IFD_EXIF) != 0;
            case IfdId.TYPE_IFD_GPS:
                return (mOptions & OPTION_IFD_GPS) != 0;
            case IfdId.TYPE_IFD_INTEROPERABILITY:
                return (mOptions & OPTION_IFD_INTEROPERABILITY) != 0;
        }
        return false;
    }
    private boolean isThumbnailRequested() {
        return (mOptions & OPTION_THUMBNAIL) != 0;
    }
    private ExifParser(InputStream inputStream, int options, ExifInterface iRef)
            throws IOException, ExifInvalidFormatException {
        if (inputStream == null) {
            throw new IOException("Null argument inputStream to ExifParser");
        }

        mInterface = iRef;
        mContainExifData = seekTiffData(inputStream);
        mTiffStream = new CountedDataInputStream(inputStream);
        mOptions = options;
        if (!mContainExifData) {
            return;
        }
        parseTiffHeader();
        long offset = mTiffStream.readUnsignedInt();
        if (offset > Integer.MAX_VALUE) {
            throw new ExifInvalidFormatException("Invalid offset " + offset);
        }
        mIfd0Position = (int) offset;
        mIfdType = IfdId.TYPE_IFD_0;
        if (isIfdRequested(IfdId.TYPE_IFD_0) || needToParseOffsetsInCurrentIfd()) {
            registerIfd(IfdId.TYPE_IFD_0, offset);
            if (offset != DEFAULT_IFD0_OFFSET) {
                mDataAboveIfd0 = new byte[(int) offset - DEFAULT_IFD0_OFFSET];
                read(mDataAboveIfd0);
            }
        }
    }
    protected static ExifParser parse(InputStream inputStream, int options, ExifInterface iRef)
            throws IOException, ExifInvalidFormatException {
        return new ExifParser(inputStream, options, iRef);
    }

    protected static ExifParser parse(InputStream inputStream, ExifInterface iRef)
            throws IOException, ExifInvalidFormatException {
        return new ExifParser(inputStream, OPTION_IFD_0 | OPTION_IFD_1
                | OPTION_IFD_EXIF | OPTION_IFD_GPS | OPTION_IFD_INTEROPERABILITY
                | OPTION_THUMBNAIL, iRef);
    }
    protected int next() throws IOException, ExifInvalidFormatException {
        if (!mContainExifData) {
            return EVENT_END;
        }
        int offset = mTiffStream.getReadByteCount();
        int endOfTags = mIfdStartOffset + OFFSET_SIZE + TAG_SIZE * mNumOfTagInIfd;
        if (offset < endOfTags) {
            mTag = readTag();
            if (mTag == null) {
                return next();
            }
            if (mNeedToParseOffsetsInCurrentIfd) {
                checkOffsetOrImageTag(mTag);
            }
            return EVENT_NEW_TAG;
        } else if (offset == endOfTags) {
            // There is a link to ifd1 at the end of ifd0
            if (mIfdType == IfdId.TYPE_IFD_0) {
                long ifdOffset = readUnsignedLong();
                if (isIfdRequested(IfdId.TYPE_IFD_1) || isThumbnailRequested()) {
                    if (ifdOffset != 0) {
                        registerIfd(IfdId.TYPE_IFD_1, ifdOffset);
                    }
                }
            } else {
                int offsetSize = 4;
                // Some camera models use invalid length of the offset
                if (mCorrespondingEvent.size() > 0) {
                    offsetSize = mCorrespondingEvent.firstEntry().getKey() -
                            mTiffStream.getReadByteCount();
                }
                if (offsetSize < 4) {
                } else {
                    long ifdOffset = readUnsignedLong();
                    if (ifdOffset != 0) {
                    }
                }
            }
        }
        while (mCorrespondingEvent.size() != 0) {
            Map.Entry<Integer, Object> entry = mCorrespondingEvent.pollFirstEntry();
            Object event = entry.getValue();
            try {
                skipTo(entry.getKey());
            } catch (IOException e) {

                continue;
            }
            if (event instanceof ExifParser.IfdEvent) {
                mIfdType = ((ExifParser.IfdEvent) event).ifd;
                mNumOfTagInIfd = mTiffStream.readUnsignedShort();
                mIfdStartOffset = entry.getKey();
                if (mNumOfTagInIfd * TAG_SIZE + mIfdStartOffset + OFFSET_SIZE > mApp1End) {
                    return EVENT_END;
                }
                mNeedToParseOffsetsInCurrentIfd = needToParseOffsetsInCurrentIfd();
                if (((ExifParser.IfdEvent) event).isRequested) {
                    return EVENT_START_OF_IFD;
                } else {
                    skipRemainingTagsInCurrentIfd();
                }
            } else if (event instanceof ExifParser.ImageEvent) {
                mImageEvent = (ExifParser.ImageEvent) event;
                return mImageEvent.type;
            } else {
                ExifParser.ExifTagEvent tagEvent = (ExifParser.ExifTagEvent) event;
                mTag = tagEvent.tag;
                if (mTag.getDataType() != ExifTag.TYPE_UNDEFINED) {
                    readFullTagValue(mTag);
                    checkOffsetOrImageTag(mTag);
                }
                if (tagEvent.isRequested) {
                    return EVENT_VALUE_OF_REGISTERED_TAG;
                }
            }
        }
        return EVENT_END;
    }

    protected void skipRemainingTagsInCurrentIfd() throws IOException, ExifInvalidFormatException {
        int endOfTags = mIfdStartOffset + OFFSET_SIZE + TAG_SIZE * mNumOfTagInIfd;
        int offset = mTiffStream.getReadByteCount();
        if (offset > endOfTags) {
            return;
        }
        if (mNeedToParseOffsetsInCurrentIfd) {
            while (offset < endOfTags) {
                mTag = readTag();
                offset += TAG_SIZE;
                if (mTag == null) {
                    continue;
                }
                checkOffsetOrImageTag(mTag);
            }
        } else {
            skipTo(endOfTags);
        }
        long ifdOffset = readUnsignedLong();
        // For ifd0, there is a link to ifd1 in the end of all tags
        if (mIfdType == IfdId.TYPE_IFD_0
                && (isIfdRequested(IfdId.TYPE_IFD_1) || isThumbnailRequested())) {
            if (ifdOffset > 0) {
                registerIfd(IfdId.TYPE_IFD_1, ifdOffset);
            }
        }
    }
    private boolean needToParseOffsetsInCurrentIfd() {
        switch (mIfdType) {
            case IfdId.TYPE_IFD_0:
                return isIfdRequested(IfdId.TYPE_IFD_EXIF) || isIfdRequested(IfdId.TYPE_IFD_GPS)
                        || isIfdRequested(IfdId.TYPE_IFD_INTEROPERABILITY)
                        || isIfdRequested(IfdId.TYPE_IFD_1);
            case IfdId.TYPE_IFD_1:
                return isThumbnailRequested();
            case IfdId.TYPE_IFD_EXIF:
                // The offset to interoperability IFD is located in Exif IFD
                return isIfdRequested(IfdId.TYPE_IFD_INTEROPERABILITY);
            default:
                return false;
        }
    }
    protected ExifTag getTag() {
        return mTag;
    }

    protected int getTagCountInCurrentIfd() {
        return mNumOfTagInIfd;
    }
    protected int getCurrentIfd() {
        return mIfdType;
    }
    protected int getStripIndex() {
        return mImageEvent.stripIndex;
    }
    protected int getStripCount() {
        return mStripCount;
    }
    protected int getStripSize() {
        if (mStripSizeTag == null)
            return 0;
        return (int) mStripSizeTag.getValueAt(0);
    }
    protected int getCompressedImageSize() {
        if (mJpegSizeTag == null) {
            return 0;
        }
        return (int) mJpegSizeTag.getValueAt(0);
    }
    private void skipTo(int offset) throws IOException {
        mTiffStream.skipTo(offset);
        while (!mCorrespondingEvent.isEmpty() && mCorrespondingEvent.firstKey() < offset) {
            mCorrespondingEvent.pollFirstEntry();
        }
    }
    protected void registerForTagValue(ExifTag tag) {
        if (tag.getOffset() >= mTiffStream.getReadByteCount()) {
            mCorrespondingEvent.put(tag.getOffset(), new ExifParser.ExifTagEvent(tag, true));
        }
    }
    private void registerIfd(int ifdType, long offset) {
        // Cast unsigned int to int since the offset is always smaller
        // than the size of APP1 (65536)
        mCorrespondingEvent.put((int) offset, new ExifParser.IfdEvent(ifdType, isIfdRequested(ifdType)));
    }
    private void registerCompressedImage(long offset) {
        mCorrespondingEvent.put((int) offset, new ExifParser.ImageEvent(EVENT_COMPRESSED_IMAGE));
    }
    private void registerUncompressedStrip(int stripIndex, long offset) {
        mCorrespondingEvent.put((int) offset, new ExifParser.ImageEvent(EVENT_UNCOMPRESSED_STRIP
                , stripIndex));
    }
    private ExifTag readTag() throws IOException, ExifInvalidFormatException {
        short tagId = mTiffStream.readShort();
        short dataFormat = mTiffStream.readShort();
        long numOfComp = mTiffStream.readUnsignedInt();
        if (numOfComp > Integer.MAX_VALUE) {
            throw new ExifInvalidFormatException(
                    "Number of component is larger then Integer.MAX_VALUE");
        }
        // Some invalid image file contains invalid data type. Ignore those tags
        if (!ExifTag.isValidType(dataFormat)) {
            mTiffStream.skip(4);
            return null;
        }
        // TODO: handle numOfComp overflow
        ExifTag tag = new ExifTag(tagId, dataFormat, (int) numOfComp, mIfdType,
                (int) numOfComp != ExifTag.SIZE_UNDEFINED);
        int dataSize = tag.getDataSize();
        if (dataSize > 4) {
            long offset = mTiffStream.readUnsignedInt();
            if (offset > Integer.MAX_VALUE) {
                throw new ExifInvalidFormatException(
                        "offset is larger then Integer.MAX_VALUE");
            }
            // Some invalid images put some undefined data before IFD0.
            // Read the data here.
            if (offset < mIfd0Position && dataFormat == ExifTag.TYPE_UNDEFINED) {
                byte[] buf = new byte[(int) numOfComp];
                System.arraycopy(mDataAboveIfd0, (int) offset - DEFAULT_IFD0_OFFSET,
                        buf, 0, (int) numOfComp);
                tag.setValue(buf);
            } else {
                tag.setOffset((int) offset);
            }
        } else {
            boolean defCount = tag.hasDefinedCount();
            // Set defined count to 0 so we can add \0 to non-terminated strings
            tag.setHasDefinedCount(false);
            // Read value
            readFullTagValue(tag);
            tag.setHasDefinedCount(defCount);
            mTiffStream.skip(4 - dataSize);
            // Set the offset to the position of value.
            tag.setOffset(mTiffStream.getReadByteCount() - 4);
        }
        return tag;
    }
    private void checkOffsetOrImageTag(ExifTag tag) {
        // Some invalid formattd image contains tag with 0 size.
        if (tag.getComponentCount() == 0) {
            return;
        }
        short tid = tag.getTagId();
        int ifd = tag.getIfd();
        if (tid == TAG_EXIF_IFD && checkAllowed(ifd, ExifInterface.TAG_EXIF_IFD)) {
            if (isIfdRequested(IfdId.TYPE_IFD_EXIF)
                    || isIfdRequested(IfdId.TYPE_IFD_INTEROPERABILITY)) {
                registerIfd(IfdId.TYPE_IFD_EXIF, tag.getValueAt(0));
            }
        } else if (tid == TAG_GPS_IFD && checkAllowed(ifd, ExifInterface.TAG_GPS_IFD)) {
            if (isIfdRequested(IfdId.TYPE_IFD_GPS)) {
                registerIfd(IfdId.TYPE_IFD_GPS, tag.getValueAt(0));
            }
        } else if (tid == TAG_INTEROPERABILITY_IFD
                && checkAllowed(ifd, ExifInterface.TAG_INTEROPERABILITY_IFD)) {
            if (isIfdRequested(IfdId.TYPE_IFD_INTEROPERABILITY)) {
                registerIfd(IfdId.TYPE_IFD_INTEROPERABILITY, tag.getValueAt(0));
            }
        } else if (tid == TAG_JPEG_INTERCHANGE_FORMAT
                && checkAllowed(ifd, ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT)) {
            if (isThumbnailRequested()) {
                registerCompressedImage(tag.getValueAt(0));
            }
        } else if (tid == TAG_JPEG_INTERCHANGE_FORMAT_LENGTH
                && checkAllowed(ifd, ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH)) {
            if (isThumbnailRequested()) {
                mJpegSizeTag = tag;
            }
        } else if (tid == TAG_STRIP_OFFSETS && checkAllowed(ifd, ExifInterface.TAG_STRIP_OFFSETS)) {
            if (isThumbnailRequested()) {
                if (tag.hasValue()) {
                    for (int i = 0; i < tag.getComponentCount(); i++) {
                        if (tag.getDataType() == ExifTag.TYPE_UNSIGNED_SHORT) {
                            registerUncompressedStrip(i, tag.getValueAt(i));
                        } else {
                            registerUncompressedStrip(i, tag.getValueAt(i));
                        }
                    }
                } else {
                    mCorrespondingEvent.put(tag.getOffset(), new ExifParser.ExifTagEvent(tag, false));
                }
            }
        } else if (tid == TAG_STRIP_BYTE_COUNTS
                && checkAllowed(ifd, ExifInterface.TAG_STRIP_BYTE_COUNTS)
                &&isThumbnailRequested() && tag.hasValue()) {
            mStripSizeTag = tag;
        }
    }
    private boolean checkAllowed(int ifd, int tagId) {
        int info = mInterface.getTagInfo().get(tagId);
        if (info == ExifInterface.DEFINITION_NULL) {
            return false;
        }
        return ExifInterface.isIfdAllowed(info, ifd);
    }


    protected void readFullTagValue(ExifTag tag) throws IOException {
        // Some invalid images contains tags with wrong size, check it here
        short type = tag.getDataType();
        if (type == ExifTag.TYPE_ASCII || type == ExifTag.TYPE_UNDEFINED ||
                type == ExifTag.TYPE_UNSIGNED_BYTE) {
            int size = tag.getComponentCount();
            if (mCorrespondingEvent.size() > 0) {
                if (mCorrespondingEvent.firstEntry().getKey() < mTiffStream.getReadByteCount()
                        + size) {
                    Object event = mCorrespondingEvent.firstEntry().getValue();
                    if (event instanceof ExifParser.ImageEvent) {
                        // Tag value overlaps thumbnail, ignore thumbnail.
                        Map.Entry<Integer, Object> entry = mCorrespondingEvent.pollFirstEntry();
                    } else {
                        // Tag value overlaps another tag, shorten count
                        if (event instanceof ExifParser.IfdEvent) {

                        } else if (event instanceof ExifParser.ExifTagEvent) {

                        }
                        size = mCorrespondingEvent.firstEntry().getKey()
                                - mTiffStream.getReadByteCount();

                        tag.forceSetComponentCount(size);
                    }
                }
            }
        }
        switch (tag.getDataType()) {
            case ExifTag.TYPE_UNSIGNED_BYTE:
            case ExifTag.TYPE_UNDEFINED:
                byte buf[] = new byte[tag.getComponentCount()];
                read(buf);
                tag.setValue(buf);
                break;
            case ExifTag.TYPE_ASCII:
                tag.setValue(readString(tag.getComponentCount()));
                break;
            case ExifTag.TYPE_UNSIGNED_LONG: {
                long value[] = new long[tag.getComponentCount()];
                for (int i = 0, n = value.length; i < n; i++) {
                    value[i] = readUnsignedLong();
                }
                tag.setValue(value);
            }
            break;
            case ExifTag.TYPE_UNSIGNED_RATIONAL: {
                com.leashtime.sitterapp.Exif.Rational value[] = new com.leashtime.sitterapp.Exif.Rational[tag.getComponentCount()];
                for (int i = 0, n = value.length; i < n; i++) {
                    value[i] = readUnsignedRational();
                }
                tag.setValue(value);
            }
            break;
            case ExifTag.TYPE_UNSIGNED_SHORT: {
                int value[] = new int[tag.getComponentCount()];
                for (int i = 0, n = value.length; i < n; i++) {
                    value[i] = readUnsignedShort();
                }
                tag.setValue(value);
            }
            break;
            case ExifTag.TYPE_LONG: {
                int value[] = new int[tag.getComponentCount()];
                for (int i = 0, n = value.length; i < n; i++) {
                    value[i] = readLong();
                }
                tag.setValue(value);
            }
            break;
            case ExifTag.TYPE_RATIONAL:
                com.leashtime.sitterapp.Exif.Rational value[] = new com.leashtime.sitterapp.Exif.Rational[tag.getComponentCount()];
                for (int i = 0, n = value.length; i < n; i++) {
                    value[i] = readRational();
                }
                tag.setValue(value);
                break;
        }
        if (LOGV) {
        }
    }
    private void parseTiffHeader() throws IOException,
            ExifInvalidFormatException {
        short byteOrder = mTiffStream.readShort();
        if (LITTLE_ENDIAN_TAG == byteOrder) {
            mTiffStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        } else if (BIG_ENDIAN_TAG == byteOrder) {
            mTiffStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        } else {
            throw new ExifInvalidFormatException("Invalid TIFF header");
        }
        if (mTiffStream.readShort() != TIFF_HEADER_TAIL) {
            throw new ExifInvalidFormatException("Invalid TIFF header");
        }
    }
    private boolean seekTiffData(InputStream inputStream) throws IOException,
            ExifInvalidFormatException {
        CountedDataInputStream dataStream = new CountedDataInputStream(inputStream);
        if (dataStream.readShort() != JpegHeader.SOI) {
            throw new ExifInvalidFormatException("Invalid JPEG format");
        }
        short marker = dataStream.readShort();
        while (marker != JpegHeader.EOI
                && !JpegHeader.isSofMarker(marker)) {
            int length = dataStream.readUnsignedShort();
            // Some invalid formatted image contains multiple APP1,
            // try to find the one with Exif data.
            if (marker == JpegHeader.APP1) {
                int header = 0;
                short headerTail = 0;
                if (length >= 8) {
                    header = dataStream.readInt();
                    headerTail = dataStream.readShort();
                    length -= 6;
                    if (header == EXIF_HEADER && headerTail == EXIF_HEADER_TAIL) {
                        mTiffStartPosition = dataStream.getReadByteCount();
                        mApp1End = length;
                        mOffsetToApp1EndFromSOF = mTiffStartPosition + mApp1End;
                        return true;
                    }
                }
            }
            if (length < 2 || length - 2 != dataStream.skip(length - 2)) {
                return false;
            }
            marker = dataStream.readShort();
        }
        return false;
    }
    protected int getOffsetToExifEndFromSOF() {
        return mOffsetToApp1EndFromSOF;
    }
    protected int getTiffStartPosition() {
        return mTiffStartPosition;
    }
    protected int read(byte[] buffer, int offset, int length) throws IOException {
        return mTiffStream.read(buffer, offset, length);
    }
    protected int read(byte[] buffer) throws IOException {
        return mTiffStream.read(buffer);
    }
    protected String readString(int n) throws IOException {
        return readString(n, US_ASCII);
    }
    protected String readString(int n, Charset charset) throws IOException {
        if (n > 0) {
            return mTiffStream.readString(n, charset);
        } else {
            return "";
        }
    }
    protected int readUnsignedShort() throws IOException {
        return mTiffStream.readShort() & 0xffff;
    }
    protected long readUnsignedLong() throws IOException {
        return readLong() & 0xffffffffL;
    }
    protected Rational readUnsignedRational() throws IOException {
        long nomi = readUnsignedLong();
        long denomi = readUnsignedLong();
        Rational itemRational = new Rational(nomi,denomi);
        return itemRational;
    }
    protected int readLong() throws IOException {
        return mTiffStream.readInt();
    }
    protected Rational readRational() throws IOException {
        int nomi = readLong();
        int denomi = readLong();
        return new com.leashtime.sitterapp.Exif.Rational(nomi, denomi);
    }
    private static class ImageEvent {
        int stripIndex;
        int type;
        ImageEvent(int type) {
            this.stripIndex = 0;
            this.type = type;
        }
        ImageEvent(int type, int stripIndex) {
            this.type = type;
            this.stripIndex = stripIndex;
        }
    }
    private static class IfdEvent {
        int ifd;
        boolean isRequested;
        IfdEvent(int ifd, boolean isInterestedIfd) {
            this.ifd = ifd;
            this.isRequested = isInterestedIfd;
        }
    }
    private static class ExifTagEvent {
        ExifTag tag;
        boolean isRequested;
        ExifTagEvent(ExifTag tag, boolean isRequireByUser) {
            this.tag = tag;
            this.isRequested = isRequireByUser;
        }
    }
    protected ByteOrder getByteOrder() {
        return mTiffStream.getByteOrder();
    }
}
