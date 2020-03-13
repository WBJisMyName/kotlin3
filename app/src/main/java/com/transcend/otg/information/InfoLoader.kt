package com.transcend.otg.information

import android.app.Application
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.loader.content.AsyncTaskLoader
import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.MetadataException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.github.mjdev.libaums.fs.UsbFile
import com.github.mjdev.libaums.fs.UsbFileStreamFactory
import com.transcend.otg.R
import com.transcend.otg.utilities.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class InfoLoader(
    private val mApplication: Application,
    private val mPath: String,
    private val mType: Int
) : AsyncTaskLoader<Boolean>(mApplication) {
    var mediaInfo: MediaInfo? = null
    var imageInfo: ImageInfo? = null
    val repository = MediaInfoRepository(mApplication)

    override fun loadInBackground(): Boolean? {
        if (FileFactory().isLocalPath(mPath) || FileFactory().isSDCardPath(context, mPath)) {
            when (mType) {
                Constant.TYPE_IMAGE -> return retrieveImage()
                Constant.TYPE_VIDEO -> return retrieveVideo()
                Constant.TYPE_MUSIC -> return retrieveMusic()
                Constant.TYPE_DIR -> return retrieveFolder()
                else -> return retrieveFile()
            }
        } else {
            val target = UsbUtils.usbFileSystem?.rootDirectory?.search(mPath)
            if (target != null){
                when (mType) {
                    Constant.TYPE_IMAGE -> return retrieveImage(target)
                    Constant.TYPE_VIDEO -> return retrieveVideo(target)
                    Constant.TYPE_MUSIC -> return retrieveMusic(target)
                    Constant.TYPE_DIR -> return retrieveFolder(target)
                    else -> return retrieveFile(target)
                }
                return true
            }
            return false
        }
    }

    private fun retrieveImage(): Boolean {
        imageInfo = repository.getImageInfo(mPath)
        if (imageInfo != null){
            return true
        }

        val file = File(mPath)
        var inputStream: InputStream? = null

        mediaInfo = MediaInfo(mPath, file.name)
        mediaInfo!!.parent = file.parent + "/"
        mediaInfo!!.size = file.length()

        try {
            inputStream = FileInputStream(file)
            val metadata = ImageMetadataReader.readMetadata(inputStream)

            // A Metadata object contains multiple Directory objects
            for (directory in metadata.getDirectories()) {
                if ("ExifSubIFDDirectory".equals(directory.javaClass.simpleName, ignoreCase = true)) {
                    //光圈F值=鏡頭的焦距/鏡頭光圈的直徑
                    //                        System.out.println("光圈值: f/" + directory.getString(ExifSubIFDDirectory.TAG_FNUMBER) );
                    //                        System.out.println("曝光時間: " + directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) + "秒" );
                    //                        System.out.println("ISO速度: " + directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) );
                    //                        System.out.println("焦距: " + directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) + "毫米" );
                    //                        System.out.println("拍照時間: " + directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL) );
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                        val dateTime = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                        mediaInfo!!.date_time = dateTime
                    }

                    //                        System.out.println("寬: " + directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH) );
                    //                        System.out.println("高: " + directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT) );
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH))
                        mediaInfo!!.width = Integer.parseInt(directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH))
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT))
                        mediaInfo!!.height = Integer.parseInt(directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT))

                    if (directory.containsTag(ExifSubIFDDirectory.TAG_FNUMBER))
                        mediaInfo!!.f_number = directory.getDouble(ExifSubIFDDirectory.TAG_FNUMBER)

                    if (directory.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                        val exposureTime =
                            directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).toString()
                        if (exposureTime.contains("/")) {    //分數
                            val exp_time = directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).toString().split("/")
                            val e0 = Integer.parseInt(exp_time[0])
                            val e1 = Integer.parseInt(exp_time[1])
                            mediaInfo!!.exposure_time_numerator = e0
                            mediaInfo!!.exposure_time_denominator = e1
                        } else {   //小數
                            var exp_time = directory.getDouble(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                            var denominator = 1
                            while (exp_time < 1) {
                                if(Int.MAX_VALUE / 10 >= denominator) { //避免超出Int最大值
                                    exp_time = exp_time * 10
                                    denominator = denominator * 10
                                } else
                                    break
                            }
                            mediaInfo!!.exposure_time_numerator = exp_time.toInt()
                            mediaInfo!!.exposure_time_denominator = denominator
                        }
                    }

                    if (directory.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                        val focal = directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)
                        if (focal.contains("/")) {    //分數
                            mediaInfo!!.focal_length = stringConverter(focal)
                        } else  //小數
                            mediaInfo!!.focal_length = java.lang.Double.parseDouble(directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH))
                    }
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT))
                        mediaInfo!!.iso_speed_rating = Integer.parseInt(directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT))
                }

                if ("ExifIFD0Directory".equals(directory.javaClass.simpleName, ignoreCase = true)) {
                    //                        System.out.println("軟體: "+ directory.getString(ExifIFD0Directory.TAG_SOFTWARE));
                    //                        System.out.println("方向: "+ directory.getString(ExifIFD0Directory.TAG_ORIENTATION));
                    //                        System.out.println("時間: "+ directory.getDate(ExifSubIFDDirectory.TAG_DATETIME));
                    //                        System.out.println("相機製造商: " + directory.getString(ExifIFD0Directory.TAG_MAKE) );
                    //                        System.out.println("相機型號: " + directory.getString(ExifIFD0Directory.TAG_MODEL) );
                    //                        System.out.println("水平分辨率: " +  directory.getString(ExifIFD0Directory.TAG_X_RESOLUTION));
                    //                        System.out.println("垂直分辨率: " + directory.getString(ExifIFD0Directory.TAG_Y_RESOLUTION) );

                    if (directory.containsTag(ExifIFD0Directory.TAG_MODEL))
                        mediaInfo!!.model = directory.getString(ExifIFD0Directory.TAG_MODEL)

                    if (directory.containsTag(ExifIFD0Directory.TAG_MAKE))
                        mediaInfo!!.make = directory.getString(ExifIFD0Directory.TAG_MAKE)

                    if (mediaInfo!!.date_time == null) {
                        if (directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME)) {
                            mediaInfo!!.date_time = directory.getString(ExifSubIFDDirectory.TAG_DATETIME)
                        }
                    }

                    if (directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
                        mediaInfo!!.orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION)

                    if (directory.containsTag(ExifIFD0Directory.TAG_SOFTWARE))
                        mediaInfo!!.software = directory.getString(ExifIFD0Directory.TAG_SOFTWARE)
                }

                if ("GpsDirectory".equals(directory.javaClass.simpleName, ignoreCase = true)) {
                    //                        System.out.println("緯度: "+directory.getString(GpsDirectory.TAG_LATITUDE));
                    //                        System.out.println("經度: "+directory.getString(GpsDirectory.TAG_LONGITUDE));

                    if (directory.containsTag(GpsDirectory.TAG_LATITUDE)) {
                        val lat = directory.getString(GpsDirectory.TAG_LATITUDE).toString().split("\\s".toRegex())
                        mediaInfo!!.latitude = stringConverter(lat[0]) + stringConverter(lat[1]) / 60 + stringConverter(lat[2]) / 3600
                    }

                    if (directory.containsTag(GpsDirectory.TAG_LATITUDE_REF)) {
                        val lat_ref = directory.getString(GpsDirectory.TAG_LATITUDE_REF)
                        if (lat_ref == "S") {
                            mediaInfo!!.latitude = -mediaInfo!!.latitude
                        }
                    }

                    if (directory.containsTag(GpsDirectory.TAG_LONGITUDE)) {
                        val lon = directory.getString(GpsDirectory.TAG_LONGITUDE).toString().split("\\s".toRegex())
                        mediaInfo!!.longitude = stringConverter(lon[0]) + stringConverter(lon[1]) / 60 + stringConverter(lon[2]) / 3600
                    }

                    if (directory.containsTag(GpsDirectory.TAG_LONGITUDE_REF)) {
                        val lat_ref = directory.getString(GpsDirectory.TAG_LONGITUDE_REF)
                        if (lat_ref == "W") {
                            mediaInfo!!.longitude = -mediaInfo!!.longitude
                        }
                    }
                }

                if (mediaInfo!!.width == -1 || mediaInfo!!.height == -1) {
                    val bitmap = BitmapFactory.decodeFile(mPath)
                    if(bitmap != null) {
                        mediaInfo!!.height = bitmap.height
                        mediaInfo!!.width = bitmap.width
                    }
                }
            }
            repository.insert(mediaInfo!!)
            buildImageInfo()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ImageProcessingException) {
            e.printStackTrace()
        } catch (e: MetadataException) {
            e.printStackTrace()
        } catch (e: RuntimeException){
            e.printStackTrace()
        }
        return false
    }

    private fun retrieveMusic(): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }

        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(mPath)
        val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
        val mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) // ms
        val bitrate =
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) // bit/s api >= 14
        val date = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

        val f = File(mPath)
        mediaInfo = MediaInfo(mPath, f.name)
        mediaInfo!!.parent = f.parent!! + "/"
        mediaInfo!!.name = f.name
        mediaInfo!!.album = album
        mediaInfo!!.artist = artist
        mediaInfo!!.genre = mime
        mediaInfo!!.release_date = date
        repository.insert(mediaInfo!!)
        return true
    }

    private fun retrieveVideo(): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }

        val f = File(mPath)
        mediaInfo = MediaInfo(mPath, f.name)
        mediaInfo!!.parent = f.parent!! + "/"
        val extractor = MediaExtractor()
        try {
            //Adjust data source as per the requirement if file, URI, etc.
            extractor.setDataSource(mPath)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime!!.startsWith("video/")) {
                    mediaInfo!!.format = mime

                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        mediaInfo!!.frame_rate =
                            format.getInteger(MediaFormat.KEY_FRAME_RATE).toString() + ""
                    }

                    if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        mediaInfo!!.width = format.getInteger(MediaFormat.KEY_WIDTH)
                    }

                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        mediaInfo!!.height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    }

                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        val duration = format.getLong(MediaFormat.KEY_DURATION)
                        val hour = (duration / (1000L * 1000L * 60L * 60L)).toInt()
                        val min = (duration / (1000L * 1000L * 60L) - hour * 60L).toInt()
                        val sec = (duration / (1000L * 1000L) - (hour.toLong() * 60L * 60L + min * 60L)).toInt()
                        mediaInfo!!.duration = String.format("%d:%02d:%02d", hour, min, sec)
                    }
                    repository.insert(mediaInfo!!)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            //Release stuff
            extractor.release()
        }

        return true
    }

    private fun retrieveFile(): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }

        val file = File(mPath)
        mediaInfo = MediaInfo(mPath, file.name)
        mediaInfo!!.parent = file.parent + "/"
        mediaInfo!!.size = file.length()
        mediaInfo!!.last_modify = file.lastModified()
        repository.insert(mediaInfo!!)
        return true
    }

    private fun retrieveFolder(): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }

        val file_directory_numbers = intArrayOf(0, 0)
        val size = getDocumentFilesNumberAndSize(file_directory_numbers)

        val f = File(mPath)
        mediaInfo = MediaInfo(mPath, f.name)
        mediaInfo!!.parent = "$mPath/"
        mediaInfo!!.last_modify = f.lastModified()
        mediaInfo!!.file_num = file_directory_numbers[0].toLong()
        mediaInfo!!.folder_num = file_directory_numbers[1].toLong()
        mediaInfo!!.size = size
        repository.insert(mediaInfo!!)
        return true
    }

    private fun retrieveImage(file: UsbFile): Boolean {
        imageInfo = repository.getImageInfo(mPath)
        if (imageInfo != null){
            return true
        }

        var inputStream: InputStream? = null
        mediaInfo = MediaInfo("Usb Device"+mPath, file.name)
        mediaInfo!!.parent = "Usb Device"+(file.parent?.absolutePath ?: "")
        mediaInfo!!.size = file.length
        try {
            inputStream = UsbFileStreamFactory.createBufferedInputStream(file, UsbUtils.usbFileSystem!!)
            val metadata = ImageMetadataReader.readMetadata(inputStream)

            // A Metadata object contains multiple Directory objects
            for (directory in metadata.getDirectories()) {
                if ("ExifSubIFDDirectory".equals(directory.javaClass.simpleName, ignoreCase = true)) {
                    //光圈F值=镜头的焦距/镜头光圈的直径
                    //                        System.out.println("光圈值: f/" + directory.getString(ExifSubIFDDirectory.TAG_FNUMBER) );
                    //                        System.out.println("曝光時間: " + directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) + "秒" );
                    //                        System.out.println("ISO速度: " + directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) );
                    //                        System.out.println("焦距: " + directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) + "毫米" );
                    //                        System.out.println("拍照時間: " + directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL) );
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
                        val dateTime = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
                        mediaInfo!!.date_time = dateTime
                    }

                    //                        System.out.println("寬: " + directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH) );
                    //                        System.out.println("高: " + directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT) );
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH))
                        mediaInfo!!.width = Integer.parseInt(directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH))
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT))
                        mediaInfo!!.height = Integer.parseInt(directory.getString(ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT))

                    if (directory.containsTag(ExifSubIFDDirectory.TAG_FNUMBER))
                        mediaInfo!!.f_number = directory.getDouble(ExifSubIFDDirectory.TAG_FNUMBER)

                    if (directory.containsTag(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)) {
                        val exposureTime = directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).toString()
                        if (exposureTime.contains("/")) {    //分數
                            val exp_time = directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).toString().split("/")
                            val e0 = Integer.parseInt(exp_time[0])
                            val e1 = Integer.parseInt(exp_time[1])
                            mediaInfo!!.exposure_time_numerator = e0
                            mediaInfo!!.exposure_time_denominator = e1
                        } else {   //小數
                            var exp_time = directory.getDouble(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)
                            var denominator = 1
                            while (exp_time < 1) {
                                exp_time = exp_time * 10
                                denominator = denominator * 10
                            }
                            mediaInfo!!.exposure_time_numerator = exp_time.toInt()
                            mediaInfo!!.exposure_time_denominator = denominator
                        }
                    }

                    if (directory.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
                        val focal = directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)
                        if (focal.contains("/"))    //分數
                            mediaInfo!!.focal_length = stringConverter(focal)
                        else    //小數
                            mediaInfo!!.focal_length = java.lang.Double.parseDouble(directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH))
                    }
                    if (directory.containsTag(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT))
                        mediaInfo!!.iso_speed_rating = Integer.parseInt(directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT))
                }

                if ("ExifIFD0Directory".equals(directory.javaClass.simpleName, ignoreCase = true)) {
                    //                        System.out.println("軟體: "+ directory.getString(ExifIFD0Directory.TAG_SOFTWARE));
                    //                        System.out.println("方向: "+ directory.getString(ExifIFD0Directory.TAG_ORIENTATION));
                    //                        System.out.println("時間: "+ directory.getDate(ExifSubIFDDirectory.TAG_DATETIME));
                    //                        System.out.println("相機製造商: " + directory.getString(ExifIFD0Directory.TAG_MAKE) );
                    //                        System.out.println("相機型號: " + directory.getString(ExifIFD0Directory.TAG_MODEL) );
                    //                        System.out.println("水平分辨率: " +  directory.getString(ExifIFD0Directory.TAG_X_RESOLUTION));
                    //                        System.out.println("垂直分辨率: " + directory.getString(ExifIFD0Directory.TAG_Y_RESOLUTION) );

                    if (directory.containsTag(ExifIFD0Directory.TAG_MODEL))
                        mediaInfo!!.model = directory.getString(ExifIFD0Directory.TAG_MODEL)

                    if (directory.containsTag(ExifIFD0Directory.TAG_MAKE))
                        mediaInfo!!.make = directory.getString(ExifIFD0Directory.TAG_MAKE)

                    if (mediaInfo!!.date_time == null) {
                        if (directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME)) {
                            mediaInfo!!.date_time = directory.getString(ExifSubIFDDirectory.TAG_DATETIME)
                        }
                    }

                    if (directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
                        mediaInfo!!.orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION)

                    if (directory.containsTag(ExifIFD0Directory.TAG_SOFTWARE))
                        mediaInfo!!.software = directory.getString(ExifIFD0Directory.TAG_SOFTWARE)
                }

                if ("GpsDirectory".equals(directory.javaClass.simpleName, ignoreCase = true)) {
                    //                        System.out.println("緯度: "+directory.getString(GpsDirectory.TAG_LATITUDE));
                    //                        System.out.println("經度: "+directory.getString(GpsDirectory.TAG_LONGITUDE));

                    if (directory.containsTag(GpsDirectory.TAG_LATITUDE)) {
                        val lat = directory.getString(GpsDirectory.TAG_LATITUDE).toString().split("\\s".toRegex())
                        mediaInfo!!.latitude =
                            stringConverter(lat[0]) + stringConverter(lat[1]) / 60 + stringConverter(lat[2]) / 3600
                    }

                    if (directory.containsTag(GpsDirectory.TAG_LATITUDE_REF)) {
                        val lat_ref = directory.getString(GpsDirectory.TAG_LATITUDE_REF)
                        if (lat_ref == "S")
                            mediaInfo!!.latitude = -mediaInfo!!.latitude
                    }

                    if (directory.containsTag(GpsDirectory.TAG_LONGITUDE)) {
                        val lon = directory.getString(GpsDirectory.TAG_LONGITUDE).toString().split("\\s".toRegex())
                        mediaInfo!!.longitude =
                            stringConverter(lon[0]) + stringConverter(lon[1]) / 60 + stringConverter(lon[2]) / 3600
                    }

                    if (directory.containsTag(GpsDirectory.TAG_LONGITUDE_REF)) {
                        val lat_ref = directory.getString(GpsDirectory.TAG_LONGITUDE_REF)
                        if (lat_ref == "W")
                            mediaInfo!!.longitude = -mediaInfo!!.longitude
                    }
                }

//                if (mediaInfo!!.width == -1 || mediaInfo!!.height == -1) {
//                    val bitmap = MediaStore.Images.Media.getBitmap(mApplication.contentResolver, file.uri)
//                    //                    Bitmap bitmap= BitmapFactory.decodeFile(mPath);
//                    if(bitmap != null) {
//                        mediaInfo!!.height = bitmap.height
//                        mediaInfo!!.width = bitmap.width
//                    }
//                }

                repository.insert(mediaInfo!!)
                buildImageInfo()
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ImageProcessingException) {
            e.printStackTrace()
        } catch (e: MetadataException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }

        return false
    }

    private fun buildImageInfo(){
        if(mediaInfo != null) {
            imageInfo = ImageInfo(mPath, mediaInfo!!.name)
            var nameSub = ""
            if (mediaInfo!!.size != -1L)
                nameSub = MathUtils.getStorageSize(mediaInfo!!.size)
            if (mediaInfo!!.width != -1 && mediaInfo!!.height != -1)
                imageInfo!!.name_subtitle =
                    mediaInfo!!.width.toString() + "x" + mediaInfo!!.height.toString() + "   $nameSub"

            if (mediaInfo!!.date_time != null) {
                val dateString = mediaInfo!!.date_time!!
                val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss")
                val date: Date = sdf.parse(dateString)!!

                //先行定義時間格式
                val date_format =
                    SimpleDateFormat(context.resources.getString(R.string.date_format))
                val week_time_format =
                    SimpleDateFormat(context.resources.getString(R.string.week_time_format))
                imageInfo!!.time_title = date_format.format(date)
                imageInfo!!.time_subtitle = week_time_format.format(date)
            }

            if (mediaInfo!!.model != null && mediaInfo!!.f_number != -1.0 && mediaInfo!!.focal_length != -1.0 && mediaInfo!!.iso_speed_rating != -1) {
                //感覺機型非必要，若沒有則顯示前者即可
                var tmp_make = ""
                if (mediaInfo!!.make != null)
                    tmp_make = ", " + mediaInfo!!.make
                imageInfo!!.device_title = mediaInfo!!.model.toString() + tmp_make

                var df = DecimalFormat("0.0")
                val f_number: Double =
                    df.format(mediaInfo!!.f_number).toDouble() //四捨五入到小數第一位

                var exposure_time = ""
                if (mediaInfo!!.exposure_time_numerator != -1 && mediaInfo!!.exposure_time_denominator != -1)
                    exposure_time =
                        mediaInfo!!.exposure_time_numerator.toString() + "/" + mediaInfo!!.exposure_time_denominator.toString()
                df = DecimalFormat("0.00")
                val focal_length: Double = df.format(mediaInfo!!.focal_length).toDouble()

                imageInfo!!.device_subtitle =
                    "f/" + f_number + ",  " + exposure_time + ",  " + focal_length + "mm,  " + "ISO" + mediaInfo!!.iso_speed_rating
            }
            if (mediaInfo!!.latitude != -1.0 && mediaInfo!!.longitude != -1.0) {
                val address = MainApplication.getInstance()!!.getAddress(
                    mediaInfo!!.latitude,
                    mediaInfo!!.longitude
                )
                if (address != null) {
                    val df = DecimalFormat("0.000000")
                    imageInfo!!.location_title = address
                    imageInfo!!.location_subtitle =
                        df.format(mediaInfo!!.latitude).toString() + ", " + df.format(
                            mediaInfo!!.longitude
                        )
                }
            }
            repository.insert(imageInfo!!)
        }
    }

    private fun retrieveMusic(file: UsbFile): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }
        mediaInfo = MediaInfo("Usb Device"+mPath, file.name)
        mediaInfo!!.parent = "Usb Device"+(file.parent?.absolutePath)
        repository.insert(mediaInfo!!)
        return true
    }

    private fun retrieveVideo(file: UsbFile): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }
        mediaInfo = MediaInfo("Usb Device"+mPath, file.name)
        mediaInfo!!.parent = "Usb Device"+(file.parent?.absolutePath)
        repository.insert(mediaInfo!!)
        return true
    }

    private fun retrieveFile(file: UsbFile): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }
        mediaInfo = MediaInfo("Usb Device"+mPath, file.name)
        mediaInfo!!.size = file.length
        mediaInfo!!.last_modify = file.lastModified()
        mediaInfo!!.parent = "Usb Device"+(file.parent?.absolutePath)
        repository.insert(mediaInfo!!)
        return true
    }

    private fun retrieveFolder(file: UsbFile): Boolean {
        mediaInfo = repository.getMediaInfo(mPath)
        if (mediaInfo != null){
            return true
        }
        val file_directory_numbers = intArrayOf(0, 0)
        val size = getDocumentFilesNumberAndSize(file_directory_numbers)

        mediaInfo = MediaInfo("Usb Device"+"$mPath/", file.name)
        mediaInfo!!.file_num = file_directory_numbers[0].toLong()
        mediaInfo!!.folder_num = file_directory_numbers[1].toLong()
        mediaInfo!!.size = size
        mediaInfo!!.parent = "Usb Device"+"$mPath"
        mediaInfo!!.last_modify = file.lastModified()
        repository.insert(mediaInfo!!)
        return true
    }

    private fun getFilesNumber(dfile: UsbFile, _file_directory_numbers: IntArray) {
        for (df in dfile.listFiles()) {
            if (df.isDirectory) {
                getFilesNumber(df, _file_directory_numbers)
                _file_directory_numbers[1]++
            } else {
                _file_directory_numbers[0]++
            }
        }
    }

    private fun getFilesNumber(file: File, _file_directory_numbers: IntArray) {
        for (f in file.listFiles()!!) {
            if (f.isDirectory) {
                getFilesNumber(f, _file_directory_numbers)
                _file_directory_numbers[1]++
            } else {
                _file_directory_numbers[0]++
            }
        }
    }

    private fun getDocumentFilesNumberAndSize(_file_directory_numbers: IntArray): Long {
        var total_size: Long = 0
        if (mPath.startsWith(Constant.LOCAL_ROOT) || (Constant.SD_ROOT != null && mPath.startsWith(Constant.SD_ROOT!!))) {
            val file = File(mPath)
            if (file.exists()) {
                for (f in file.listFiles()!!) {
                    if (f.isDirectory) {
                        getFilesNumber(f, _file_directory_numbers)
                        _file_directory_numbers[1]++
                    } else {
                        _file_directory_numbers[0]++
                        total_size += f.length()
                    }
                }
            }
        } else {
            val file = UsbUtils.usbFileSystem?.rootDirectory?.search(mPath)
            if (file != null) {
                for (f in file.listFiles()) {
                    if (f.isDirectory) {
                        getFilesNumber(f, _file_directory_numbers)
                        _file_directory_numbers[1]++
                    } else {
                        _file_directory_numbers[0]++
                        total_size += f.length
                    }
                }
            }
        }
        return total_size
    }

    private fun stringConverter(tmp: String): Double {
        val fraction = tmp.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return java.lang.Double.parseDouble(fraction[0]) / java.lang.Double.parseDouble(fraction[1])
    }
}
