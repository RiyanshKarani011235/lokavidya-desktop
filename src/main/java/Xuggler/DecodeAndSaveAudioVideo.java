/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Xuggler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;


public class DecodeAndSaveAudioVideo {

 public static void convertFormat(String fromFileName,String toFileName) {
	 System.out.println("Converting from "+fromFileName+" to "+toFileName);
	 IMediaReader reader = ToolFactory.makeReader(fromFileName);
	 reader.addListener(ToolFactory.makeWriter(toFileName, reader));
	 while (reader.readPacket() == null);
 }

 public static void main(String[] args) {}

 public static void stitch(String filenamevideo,String filenameaudio,final String outputName) {
	 	IMediaWriter mWriter = ToolFactory.makeWriter(outputName); //output file

	    IContainer containerVideo = IContainer.make();
	    IContainer containerAudio = IContainer.make();

	    if (containerVideo.open(filenamevideo, IContainer.Type.READ, null) < 0)
	        throw new IllegalArgumentException("Cant find " + filenamevideo);

	    if (containerAudio.open(filenameaudio, IContainer.Type.READ, null) < 0)
	        throw new IllegalArgumentException("Cant find " + filenameaudio);

	    int numStreamVideo = containerVideo.getNumStreams();
	    int numStreamAudio = containerAudio.getNumStreams();

	    System.out.println("Number of video streams: "+numStreamVideo + "\n" + "Number of audio streams: "+numStreamAudio );

		int videostreamt = -1; // this is the video stream id
		int audiostreamt = -1;
		
		IStreamCoder  videocoder = null;

	    for(int i=0; i<numStreamVideo; i++) {
	        IStream stream = containerVideo.getStream(i);
	        IStreamCoder code = stream.getStreamCoder();

	        if(code.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
	            videostreamt = i;
	            videocoder = code;
	            break;
	        }
	        
	    }

	    for(int i=0; i<numStreamAudio; i++) {
	        IStream stream = containerAudio.getStream(i);
	        IStreamCoder code = stream.getStreamCoder();

	        if(code.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
	            audiostreamt = i;
	            break;
	        }
	    }

	    if (videostreamt == -1) throw new RuntimeException("No video stream found");
	    if (audiostreamt == -1) throw new RuntimeException("No audio stream found");

	    if(videocoder.open()<0 ) throw new RuntimeException("Cant open video coder");
	    IPacket packetvideo = IPacket.make();

	    IStreamCoder audioCoder = containerAudio.getStream(audiostreamt).getStreamCoder();

	    if(audioCoder.open()<0 ) throw new RuntimeException("Cant open audio coder");
	    mWriter.addAudioStream(1, 1, audioCoder.getChannels(), audioCoder.getSampleRate());

	    mWriter.addVideoStream(0, 0, videocoder.getWidth(), videocoder.getHeight());

	    IPacket packetaudio = IPacket.make();

	    while(containerVideo.readNextPacket(packetvideo) >= 0 ||
	            containerAudio.readNextPacket(packetaudio) >= 0){

	        if(packetvideo.getStreamIndex() == videostreamt){

	            //video packet
	            IVideoPicture picture = IVideoPicture.make(videocoder.getPixelType(),
	                    videocoder.getWidth(),
	                    videocoder.getHeight());
	            int offset = 0;
	            while (offset < packetvideo.getSize()){
	                int bytesDecoded = videocoder.decodeVideo(picture, 
	                        packetvideo, 
	                        offset);
	                if(bytesDecoded < 0) throw new RuntimeException("bytesDecoded not working");
	                offset += bytesDecoded;

	                if(picture.isComplete()){
	                    System.out.println(picture.getPixelType());
	                    mWriter.encodeVideo(0, picture);

	                }
	            }
	        } 

	        if(packetaudio.getStreamIndex() == audiostreamt){   
	        //audio packet

	            IAudioSamples samples = IAudioSamples.make(512, 
	                    audioCoder.getChannels(),
	                    IAudioSamples.Format.FMT_S32);  
	            int offset = 0;
	            while(offset<packetaudio.getSize())
	            {
	                int bytesDecodedaudio = audioCoder.decodeAudio(samples, 
	                        packetaudio,
	                        offset);
	                if (bytesDecodedaudio < 0)
	                    throw new RuntimeException("could not detect audio");
	                offset += bytesDecodedaudio;

	                if (samples.isComplete()){
	                     mWriter.encodeAudio(1, samples);

	        		}
	            }
	    	}
	  	}
 	}
}