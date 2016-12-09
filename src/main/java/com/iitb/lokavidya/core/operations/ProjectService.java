package com.iitb.lokavidya.core.operations;

import gui.Call;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import poi.PptToImages;
import poi.PptxToImages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.iitb.lokavidya.core.data.Project;
import com.iitb.lokavidya.core.data.Segment;
import com.iitb.lokavidya.core.data.Slide;
import com.iitb.lokavidya.core.utils.FFMPEGWrapper;
import com.iitb.lokavidya.core.utils.GeneralUtils;

import Dialogs.OpenPresentation;

public class ProjectService {

	private static List<String> pptPaths=new ArrayList();
	public static Project createNewProject(String projectURL)
	{
		if(new File(projectURL).isDirectory())
			try {
				System.out.println("Deleting");
				FileUtils.cleanDirectory(new File(projectURL+File.separator));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else
		{
			System.out.println("Returning null");
			return null;
		}

		Project project = new Project();
		String projectName = new File(projectURL).getName();
		System.out.println(projectName);
		project.setProjectName(projectName);
		System.out.println(projectURL);
		project.setProjectURL(projectURL);

		//List<Segment> segmentList = new ArrayList<Segment>();
		List<Integer> orderingSequence = new ArrayList<Integer>();
		project.setOrderingSequence(orderingSequence);
		
		Segment segment = new Segment(projectURL);
		project.addSegment(segment);
		
		ProjectService.persist(project);
		GeneralUtils.stopOfficeInstance();
		return project;

	}
	
	public static Project getInstance(String pathToProjectJson)
    {
    	System.out.println("Retrieving instance from JSON: Start");
    	
    	
    	Project project = null;
        Gson gson = new Gson();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToProjectJson));
            project = gson.fromJson(bufferedReader, new TypeToken<Project>() {
            }.getType());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
			return null;
        }
        System.out.println("Retrieving instance from JSON:END");
        
        try{
	    	FileInputStream fis = new FileInputStream(pathToProjectJson);
	    	String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
	    	fis.close();
	    	if(!md5.equals(FileUtils.readFileToString(new File(FilenameUtils.concat(project.getProjectURL(), project.getProjectName()+".hash")), "UTF-8")))
	    	{
	    		System.out.println("Corrupt JSON");
	    		return null;
	    	}
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
			return null;
    	}
        
        File jsonFile = new File(pathToProjectJson);
        File projectFolder=jsonFile.getParentFile();
        String oldprojectPath=project.getProjectURL();
        if(!projectFolder.getAbsolutePath().equals(oldprojectPath))
	    {
        	System.out.println("Changing");
        	project.setProjectURL(projectFolder.getAbsolutePath());
	        List<Segment> segmentList = project.getOrderedSegmentList();
	        for(int i=0;i<segmentList.size();i++)
	        {
	        	if (segmentList.get(i).getSlide()!=null)
	        	{
	        		segmentList.get(i).getSlide().setImageURL(segmentList.get(i).getSlide().getImageURL().replace(oldprojectPath, projectFolder.getAbsolutePath()));
	        		segmentList.get(i).getSlide().setPptURL(segmentList.get(i).getSlide().getPptURL().replace(oldprojectPath, projectFolder.getAbsolutePath()));
	        		if(segmentList.get(i).getSlide().getAudio()!=null)
	        		{
		        		segmentList.get(i).getSlide().getAudio().setAudioURL(segmentList.get(i).getSlide().getAudio().getAudioURL().replace(oldprojectPath, projectFolder.getAbsolutePath()));
		        		FFMPEGWrapper ffmpegwrapper = new FFMPEGWrapper();
		        		long duration = ffmpegwrapper.getDuration(segmentList.get(i).getSlide().getAudio().getAudioURL());
		        		segmentList.get(i).setTime(duration);
	        		}
	        	}
	        	if (segmentList.get(i).getVideo()!=null)
	        	{
	        		segmentList.get(i).getVideo().setVideoURL(segmentList.get(i).getVideo().getVideoURL().replace(oldprojectPath, projectFolder.getAbsolutePath()));
	        		FFMPEGWrapper ffmpegwrapper = new FFMPEGWrapper();
	        		long duration = ffmpegwrapper.getDuration(segmentList.get(i).getVideo().getVideoURL());
	        		segmentList.get(i).setTime(duration);
	        	}
	        	
	        }
	    }
        
        ProjectService.persist(project);
        System.out.println("Returning");
        return project;
    }
	
	public static void persist(Project project)
	{
		File jsonFile= new File(FilenameUtils.concat(project.getProjectURL(), project.getProjectName()+".json"));
		if(!jsonFile.exists())
		{
			try {
				jsonFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();	
			}
		}
    	try {    
			Writer writer = new FileWriter(jsonFile.getAbsolutePath());
            Gson gson = new GsonBuilder().create();
            gson.toJson(project, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    	
    	try{
	    	FileInputStream fis = new FileInputStream(FilenameUtils.concat(project.getProjectURL(), project.getProjectName()+".json"));
	    	String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
	    	fis.close();
	    	Writer writer = new FileWriter(FilenameUtils.concat(project.getProjectURL(), project.getProjectName()+".hash"));
	    	writer.write(md5);
	    	writer.close();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
	
	public static void main(String[] args)
	{
		GeneralUtils.convertImageToPresentation("/home/frg/Documents/eighteen/ecezznvr5a.png", "/home/frg/Desktop/abc.odp");

//		System.out.println(System.getProperty("java.io.tmpdir"));
		Project test= ProjectService.getInstance("/home/frg/Documents/seven/seven.json");
		
		
//
//		
////
//		Segment segment4 = new Segment(test.getProjectURL());
//		Video video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
//		segment4.setVideo(video);
//		test.addSegment(segment4);
//		ProjectService.persist(test);
////		importAndroidProject("/home/sanket/Documents/asd/asd.json", "/home/sanket/Documents/material-required.zip");
//		System.out.println(test);
		//importPresentation("/home/sanket/Documents/Lokavidya_Desktop_Application using LibreOffice.pptx", test);
		//SegmentService.deleteImage(segment);
		//ProjectService.persist(test);
		//ProjectService.exportAndroidProject("/home/sanket/Documents/asd", "/home/sanket/Documents");
	//	ProjectService.importAndroidProject("/home/sanket/Documents/pazx/pazx.json", "/home/sanket/Documents/material-required.zip");
		//ProjectService.exportAndroidProject("/home/sanket/Documents/okzjxclkj", "/home/sanket/Documents");
			
	/*	Segment segment5 = new Segment(test.getProjectURL());
		video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
		System.out.println("VideoURL"+video.getVideoURL());
		segment2.setVideo(video);
		test.addSegment(segment5);*/

/*		
//		System.out.println(System.getProperty("java.io.tmpdir"));
		Project test= ProjectService.createNewProject("/home/sanket/Documents/asd");
//  	Segment segment = new Segment("/home/sanket/Documents/asd");
//		
//		Slide slide = new Slide("/home/sanket/Documents/test.png",test.getProjectURL());
//		Audio audio = new Audio("/home/sanket/Documents/testsunday/audio/testsunday.1.wav",test.getProjectURL());
//		slide.setAudio(audio);
//		List<Reference> referencesList = new ArrayList<Reference> ();
//		Reference reference = new Reference();
//		reference.setVideoID("123");
//		referencesList.add(reference);
//		segment.setReferences(new HashSet<Reference>(referencesList));
//		segment.setSlide(slide);
//		test.addSegment(segment);
//	
//		
//		
//
//		Segment segment2 = new Segment("/home/sanket/Documents/asd");
//		Video video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
//		System.out.println("VideoURL"+video.getVideoURL());
//		segment2.setVideo(video);
//		test.addSegment(segment2);
//		ProjectService.persist(test);
//		
//		slide = new Slide("/home/sanket/Documents/test.png",test.getProjectURL());
//		audio = new Audio("/home/sanket/Documents/testsunday/audio/testsunday.1.wav",test.getProjectURL());
//		slide.setAudio(audio);
//		referencesList = new ArrayList<Reference> ();
//		reference = new Reference();
//		reference.setVideoID("123");
//		referencesList.add(reference);
//		segment2.setReferences(new HashSet<Reference>(referencesList));
//		segment2.setSlide(slide);
//		test.addSegment(segment2);
//
//		Segment segment3 = new Segment("/home/sanket/Documents/asd");
//		video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
//		System.out.println("VideoURL"+video.getVideoURL());
//		segment2.setVideo(video);
//		test.addSegment(segment3);
//		
//		
//		slide = new Slide("/home/sanket/Documents/test.png",test.getProjectURL());
//		audio = new Audio("/home/sanket/Documents/testsunday/audio/testsunday.1.wav",test.getProjectURL());
//		slide.setAudio(audio);
//		referencesList = new ArrayList<Reference> ();
//		reference = new Reference();
//		reference.setVideoID("123");
//		referencesList.add(reference);
//		segment3.setReferences(new HashSet<Reference>(referencesList));
//		segment3.setSlide(slide);
//		test.addSegment(segment3);
//		
//
//		Segment segment4 = new Segment("/home/sanket/Documents/asd");
//		video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
//		System.out.println("VideoURL"+video.getVideoURL());
//		segment2.setVideo(video);
//		test.addSegment(segment4);
//		
//		Segment segment5 = new Segment("/home/sanket/Documents/asd");
//		video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
//		System.out.println("VideoURL"+video.getVideoURL());
//		segment2.setVideo(video);
//		test.addSegment(segment5);
//		
//		test.swapSegment(segment, segment2);
//		test.swapSegment(segment3, segment2);
//		test=ProjectService.getInstance("/home/sanket/Documents/asd/asd.json");
//
//		
//		video = new Video("/home/sanket/Desktop/sample.mp4",test.getProjectURL());
//		segment4.setVideo(video);
//		test.addSegment(segment4);
//		
		importPresentation("/home/sanket/Documents/Lokavidya_Desktop_Application using LibreOffice.pptx", test);
		//SegmentService.deleteImage(segment);
		//ProjectService.persist(test);
		//ProjectService.exportAndroidProject("/home/sanket/Documents/asd", "/home/sanket/Documents");
		//ProjectService.importAndroidProject("/home/sanket/Desktop", "/home/sanket/Documents/asd.zip");

>>>>>>> e136ecc1c28748abbbb0e8fedbf5338b1c4f2b38
		
	//	test.swapSegment(segment, segment2);
	//	test.swapSegment(segment3, segment2);
		//test=ProjectService.getInstance("/home/sanket/Documents/asd/asd.json");
	//	test=ProjectService.getInstance("/Users/SidRama/Documents/asd/asd.json");
		

		//SegmentService.deleteImage(segment);
		ProjectService.persist(test);
		System.out.println("Stitching now.... "); */
//		ProjectOperations.stitch(test);
//		System.out.println("Stitching done... ");
	//	ProjectService.exportAndroidProject("/home/sanket/Documents/asd", "/home/sanket/Documents");
	//	ProjectService.importAndroidProject("/home/sanket/Desktop", "/home/sanket/Documents/asd.zip");
	//	ProjectService.exportAndroidProject("/Users/SidRama/Documents/asd", "/Users/SidRama/Documents");
	//	ProjectService.importAndroidProject("/Users/SidRama/Desktop", "/Users/SidRama/Documents/asd.zip");
	

		//GeneralUtils.convertImageToPresentation("/home/sanket/Downloads/aPDxmvV_700b_v1.jpg", "/home/sanket/Documents/test.odp");
		//GeneralUtils.convertPresentationToImage("/home/sanket/Documents/test.odp", "/home/sanket/Documents/test.png");

		System.exit(0);

	}
	
	
	public static void exportAndroidProject(String projectPath,String androidProjectPath)
	{
		Project project = ProjectService.getInstance(projectPath+File.separator+FilenameUtils.getName(projectPath)+".json"); 
		String tmpPath=System.getProperty("java.io.tmpdir");
		File projectTmp = new File(tmpPath,project.getProjectName());
		FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper();
		if(projectTmp.exists())
			try {
				FileUtils.deleteDirectory(projectTmp);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		projectTmp.mkdir();
		File projectTmpImage = new File(projectTmp.getAbsolutePath(),"images");
		if(!projectTmpImage.exists())
			projectTmpImage.mkdir();
		File projectTmpAudio = new File(projectTmp.getAbsolutePath(),"audio");
		if(!projectTmpAudio.exists())
			projectTmpAudio.mkdir();
		List<Segment> segmentList=project.getOrderedSegmentList();
		int index=1;
		String wavPath = new File(projectTmpAudio,"temp.wav").getAbsolutePath();
		for (Segment s:segmentList)
		{
			System.out.println("HIT!");
			if(s.getSlide()!=null)
			{	
				try {
					FileUtils.copyFile(new File(s.getSlide().getImageURL()), new File(projectTmpImage,project.getProjectName()+"."+index+"."+FilenameUtils.getExtension(s.getSlide().getImageURL())));
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					if (s.getSlide().getAudio()!=null) {
						File file = new File(wavPath);
						if (file.exists())
							file.delete();
						ffmpegWrapper.convertMp3ToWav(s.getSlide().getAudio().getAudioURL(), wavPath);
						FileUtils.copyFile(new File(wavPath),
								new File(projectTmpAudio, project.getProjectName() + "." + index + "."
										+ FilenameUtils.getExtension(s.getSlide().getAudio().getAudioURL())));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				index++;
			}
			
		}
		File file = new File(wavPath);
		if (file.exists()) 
			file.delete();

		// Convert all JPG files to PNG files - ironstein - 22-11-16
		String imagesFolderPath = new File(new File(androidProjectPath, Paths.get(new File(projectPath).getAbsolutePath()).getFileName().toString()), "images").getAbsolutePath();
		System.out.println(projectTmpImage.toString());
		File[] paths = projectTmpImage.listFiles();
		System.out.println("ironstein here");

		for(int i=0; i < paths.length; i++) {
			// read a jpeg from a inputFile
			BufferedImage bufferedImage;
			try {
				if(FilenameUtils.getExtension(paths[i].getAbsolutePath()).equals("jpg")) {
					bufferedImage = ImageIO.read(paths[i]);
					String outputFile = new File(projectTmpImage, FilenameUtils.removeExtension(paths[i].getName())).getAbsolutePath() + ".png";
					System.out.println(outputFile);
					ImageIO.write(bufferedImage, "png", new File(outputFile));
					paths[i].delete();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	
		GeneralUtils.createZip(projectTmp.getAbsolutePath(), androidProjectPath);
	}

	public static void importAndroidProject(String projectjsonpath,String zipPath) {
		
		if(!new File(zipPath).exists() || !FilenameUtils.getName(zipPath).endsWith(".zip")) {
			JOptionPane.showMessageDialog(null, "invalid project path : " + zipPath + "\nandroid project should be a valid zip file");
			return;
		} else {
			System.out.println("zip file : " + zipPath);
		}
		Project project = getInstance(projectjsonpath);
		String tmpPath=System.getProperty("java.io.tmpdir");
		GeneralUtils.extractZip(zipPath, tmpPath);
		String projTmpDir =FilenameUtils.getBaseName(zipPath);
		File projectTmp= new File(tmpPath,projTmpDir);
		File projectTmpImage = new File(projectTmp.getAbsolutePath(),"images");
		File projectTmpAudio = new File(projectTmp.getAbsolutePath(),"audio");
		Comparator<File> fc = new Comparator<File>(){
	        public int compare(File o1, File o2) {
	            int i1,i2;
	            
	            i1= Integer.parseInt(o1.getName().split("\\.")[1]);
	            
	            i2= Integer.parseInt(o2.getName().split("\\.")[1]);
	            
	            System.out.println(""+i1+i2);
	            if(i1>i2)
	            {
	            	return 1;
	            }
	            if(i1==i2)
	            {
	            	return 0;
	            }
	            else
	            	return -1;
	        }
	    };
		
		if(projectTmpAudio.listFiles().length==projectTmpAudio.listFiles().length)
		{
			int i=0;
			String audioExtension =FilenameUtils.getExtension(projectTmpAudio.listFiles()[0].getAbsolutePath());
			String imageExtension =FilenameUtils.getExtension(projectTmpImage.listFiles()[0].getAbsolutePath());
			List<File> audioFilesList= Arrays.asList(projectTmpAudio.listFiles());
			Collections.sort(audioFilesList,fc);
			for(File f: audioFilesList)
			{
				String name= FilenameUtils.getBaseName(f.getAbsolutePath()).split("\\.")[0];
				String index=FilenameUtils.getBaseName(f.getAbsolutePath()).split("\\.")[1];

				// changed on 20-10-16 by ironstein
				// use both jpg and png files to create a new project

				File imageFilePng =  new File(projectTmpImage,name+"."+index+".png");
				File imageFileJpg = new File(projectTmpImage, name+"."+index+".jpg");
				if(imageFilePng.exists())
				{
					Segment segment = new Segment(imageFilePng.getAbsolutePath(),f.getAbsolutePath(),project.getProjectURL());
					System.out.println("URL of the presentation:"+segment.getSlide().getPptURL());
					project.addSegment(segment);
					
				} else if(imageFileJpg.exists()) {
					Segment segment = new Segment(imageFileJpg.getAbsolutePath(),f.getAbsolutePath(),project.getProjectURL());
					System.out.println("URL of the presentation:"+segment.getSlide().getPptURL());
					project.addSegment(segment);
				}
				if(Call.workspace.cancelled)
					break;
			}
		}
		GeneralUtils.stopOfficeInstance();
		if(Call.workspace.cancelled)
			return;
		ProjectService.persist(project);
	}
	
	public static void importPresentation(String presentationURL, Project project, OpenPresentation window)
	{
		BufferedWriter bw = null;
		//String tempPath = System.getProperty("java.io.tmpdir");
		try {
			bw=new BufferedWriter(new FileWriter("timelog.txt",true));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String tempPath = project.getProjectURL();
		File file = new File(presentationURL);
		try{
			long current_time = System.currentTimeMillis(),new_time,first_time= System.currentTimeMillis();

			int size=0;
			System.out.println("Calling Create presentation");
			Future<String> res = createPresentations(presentationURL, tempPath, file,window);
			System.out.println("Exiting Create presentation");
			if(presentationURL.endsWith(".pptx")){
			XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(file));
			
				new PptxToImages(presentationURL,project.getProjectURL());
				List<XSLFSlide> slides = ppt.getSlides();
				size=slides.size();
				window.setprogressvalue(50);
			}
			else if(presentationURL.endsWith(".ppt")){
				System.out.println("Entering for ppt");
				FileInputStream out = new FileInputStream(file);
				HSLFSlideShow ppt = new HSLFSlideShow(out);
				System.out.println("Created ppt instance");
				new PptToImages(presentationURL,project.getProjectURL());
				System.out.println("Converted to Images");
				List<HSLFSlide> slides = ppt.getSlides();
				size=slides.size();
				System.out.println("Size of slide is: "+size);
				window.setprogressvalue(50);
			}
			//System.out.println("It created ppt");
			
			//int size=slides.size();
			List<Segment> newSegments=new ArrayList<Segment>();
			double divider = (double)20/(double)size;
			int value = 50;
			for(int i=0;i<size;i++)
			{
				if(Call.workspace.cancelled)
				{
					System.out.println("Alert caught before res.get");
					return;
				}
				else
				{
					String tempImgPath = new File(project.getProjectURL(),
							("img_" + (Integer.toString(i + 1)) + ".jpg"))
							.getAbsolutePath();
					Segment segment = new Segment(project.getProjectURL(),
							false);
					System.out.println("Creating" + tempImgPath);
					Slide slide = new Slide(tempImgPath,
							project.getProjectURL(), false);
					new_time = System.currentTimeMillis();
					bw.write("\nTime taken for image creation is: "
							+ (new_time - current_time));
					current_time = new_time;
					segment.setSlide(slide);
					newSegments.add(segment);
					project.addSegment(segment);
					ProjectService.persist(project);
					new_time = System.currentTimeMillis();
					bw.write("\nTime taken for persistance is: "
							+ (new_time - current_time));
					current_time = new_time;
					//pop UI
					
					
				}
				value = (int)(50 + (double)(i+1)*(divider));
				window.setprogressvalue(value);
			}
			
			res.get();
			divider = (double)5/(double)newSegments.size();
			GeneralUtils.stopOfficeInstance();
			String presentationName= FilenameUtils.getBaseName(presentationURL);
			int i=0;
			for(Segment s: newSegments)
			{
				String pptPath="";
					pptPath=new File(tempPath,presentationName+"."+i+"."+FilenameUtils.getExtension(presentationURL)).getAbsolutePath();
				
				System.out.println(pptPath);
				SegmentService.addPresentation(project, s, pptPath);
				value = (int)(70+(double)(i+1)*(divider));
				window.setprogressvalue(value);
				i++;
				
			}
			
			
				new_time=System.currentTimeMillis();
			bw.write("\nTime taken for updating segments: "+(new_time-current_time));
			current_time=new_time;
			bw.write("Total time: "+(current_time-first_time));
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static Future<String>  createPresentations(String presentationURL,
			String tempPath, File file,OpenPresentation window) throws IOException,
			FileNotFoundException {
		BufferedWriter bw = null;
		//String tempPath = System.getProperty("java.io.tmpdir");
		try {
			bw=new BufferedWriter(new FileWriter("timelog.txt",true));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		long new_time,current_time;
		FileInputStream out = new FileInputStream(file);
		if(presentationURL.endsWith(".pptx")){
		XMLSlideShow ppt = new XMLSlideShow(out);
		
		List<XSLFSlide> slides = ppt.getSlides();
		String presentationName= FilenameUtils.getBaseName(presentationURL);
		current_time=System.currentTimeMillis();
		double divider = (double)30/(double)slides.size();
		int value = 10;
		for(int i=0;i<slides.size();i++)
		{
			if(!Call.workspace.cancelled)
			{
				File tempPPTPath = new File(tempPath,presentationName+"."+i+"."+FilenameUtils.getExtension(presentationURL));
				FileUtils.copyFile(new File(presentationURL),tempPPTPath);
				new_time=System.currentTimeMillis();
				bw.write("\nTime taken for copying the ppt: "+(new_time-current_time));
				current_time=new_time;
				
				keepSlide(tempPPTPath.getAbsolutePath(), i);
				new_time=System.currentTimeMillis();
				bw.write("\nTime taken for poi to seperate the prsentation is: "+(new_time-current_time));
				current_time=new_time;
			}
			else
			{
				System.out.println("Alert caught");
				break;
			}
			value = (int)(10+ (double)(i+1)*divider);
			window.setprogressvalue(value);
		}
		
		}
		else if(presentationURL.endsWith(".ppt")){
			
			HSLFSlideShow ppt = new HSLFSlideShow(out);
			System.out.println("Creating slides");
			List<HSLFSlide> slides = ppt.getSlides();
			
			String presentationName= FilenameUtils.getBaseName(presentationURL);
			current_time=System.currentTimeMillis();
			double divider = (double)30/(double)slides.size();
			int value = 10;
			for(int i=0;i<slides.size();i++)
			{
				if(!Call.workspace.cancelled)
				{
					File tempPPTPath = new File(tempPath,presentationName+"."+i+"."+FilenameUtils.getExtension(presentationURL));
					FileUtils.copyFile(new File(presentationURL),tempPPTPath);
					new_time=System.currentTimeMillis();
					bw.write("\nTime taken for copying the ppt: "+(new_time-current_time));
					System.out.println("\nTime taken for copying the ppt: "+(new_time-current_time));
					current_time=new_time;
					
					keepSlide(tempPPTPath.getAbsolutePath(), i+1);
					new_time=System.currentTimeMillis();
					bw.write("\nTime taken for poi to seperate the prsentation is: "+(new_time-current_time));
					System.out.println("\nTime taken for poi to seperate the prsentation is: "+(new_time-current_time));
					current_time=new_time;
				}
				else
				{
					System.out.println("Alert caught");
					break;
				}
				value = (int)(10+ (double)(i+1)*divider);
				window.setprogressvalue(value);
			}
		}
		//ppt.close();
		bw.close();
		Future<String> v=new Future<String>() {
			
	
			public boolean isDone() {
				// TODO Auto-generated method stub
				return false;
			}
			
	
			public boolean isCancelled() {
				// TODO Auto-generated method stub
				return false;
			}
			
	
			public String get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException {
				// TODO Auto-generated method stub
				return null;
			}
			
		
			public String get() throws InterruptedException, ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
			
		
			public boolean cancel(boolean mayInterruptIfRunning) {
				// TODO Auto-generated method stub
				return false;
			}
		};
        return v;
	}
	public static Future<String>  createPresentations(String presentationURL,
			String tempPath, File file) throws IOException,
			FileNotFoundException {
		BufferedWriter bw = null;
		//String tempPath = System.getProperty("java.io.tmpdir");
		try {
			bw=new BufferedWriter(new FileWriter("timelog.txt",true));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		long new_time,current_time;
		FileInputStream out = new FileInputStream(file);
		if(presentationURL.endsWith(".pptx")){
		XMLSlideShow ppt = new XMLSlideShow(out);
		
		List<XSLFSlide> slides = ppt.getSlides();
		String presentationName= FilenameUtils.getBaseName(presentationURL);
		current_time=System.currentTimeMillis();
	
		for(int i=0;i<slides.size();i++)
		{
			if(!Call.workspace.cancelled)
			{
				File tempPPTPath = new File(tempPath,presentationName+"."+i+"."+FilenameUtils.getExtension(presentationURL));
				FileUtils.copyFile(new File(presentationURL),tempPPTPath);
				new_time=System.currentTimeMillis();
				bw.write("\nTime taken for copying the ppt: "+(new_time-current_time));
				current_time=new_time;
				
				keepSlide(tempPPTPath.getAbsolutePath(), i);
				new_time=System.currentTimeMillis();
				bw.write("\nTime taken for poi to seperate the prsentation is: "+(new_time-current_time));
				current_time=new_time;
			}
			else
			{
				System.out.println("Alert caught");
				break;
			}
		
		}
		
		}
		else if(presentationURL.endsWith(".ppt")){
			
			HSLFSlideShow ppt = new HSLFSlideShow(out);
			System.out.println("Creating slides");
			List<HSLFSlide> slides = ppt.getSlides();
			
			String presentationName= FilenameUtils.getBaseName(presentationURL);
			current_time=System.currentTimeMillis();
			for(int i=0;i<slides.size();i++)
			{
				if(!Call.workspace.cancelled)
				{
					File tempPPTPath = new File(tempPath,presentationName+"."+i+"."+FilenameUtils.getExtension(presentationURL));
					FileUtils.copyFile(new File(presentationURL),tempPPTPath);
					new_time=System.currentTimeMillis();
					bw.write("\nTime taken for copying the ppt: "+(new_time-current_time));
					System.out.println("\nTime taken for copying the ppt: "+(new_time-current_time));
					current_time=new_time;
					
					keepSlide(tempPPTPath.getAbsolutePath(), i+1);
					new_time=System.currentTimeMillis();
					bw.write("\nTime taken for poi to seperate the prsentation is: "+(new_time-current_time));
					System.out.println("\nTime taken for poi to seperate the prsentation is: "+(new_time-current_time));
					current_time=new_time;
				}
				else
				{
					System.out.println("Alert caught");
					break;
				}

			}
		}
		//ppt.close();
		bw.close();
		Future<String> v=new Future<String>() {
			
	
			public boolean isDone() {
				// TODO Auto-generated method stub
				return false;
			}
			
	
			public boolean isCancelled() {
				// TODO Auto-generated method stub
				return false;
			}
			
	
			public String get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException {
				// TODO Auto-generated method stub
				return null;
			}
			
		
			public String get() throws InterruptedException, ExecutionException {
				// TODO Auto-generated method stub
				return null;
			}
			
		
			public boolean cancel(boolean mayInterruptIfRunning) {
				// TODO Auto-generated method stub
				return false;
			}
		};
        return v;
	}
	
	
	public static void keepSlide(String presentationPath, int index)
	{
		File file = new File(presentationPath);
		try {
			if(presentationPath.endsWith(".pptx")){
			XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(file));
			System.out.println(ppt.getSlides().size());
			List<XSLFSlide> slides = ppt.getSlides();
			XSLFSlide selectesdslide= slides.get(index);
			ppt.setSlideOrder(selectesdslide, 0);
			FileOutputStream out = new FileOutputStream(file);
			ppt.write(out);
		    out.close();
		    ppt = new XMLSlideShow(new FileInputStream(file));
		    slides = ppt.getSlides();
		    int i = slides.size()-1;
			while(i>0)
			{
				ppt.removeSlide(i);
				i--;
			}
			out = new FileOutputStream(file);
			//Saving the changes to the presentation
			System.out.println(ppt.getSlides().size());
		    ppt.write(out);
		    out.close();
			}
			else if(presentationPath.endsWith(".ppt")){
				HSLFSlideShow ppt = new HSLFSlideShow(new FileInputStream(file));
				//System.out.println(ppt.getSlides().size());
				List<HSLFSlide> slides = ppt.getSlides();
				ppt.reorderSlide(index, 1);
			
				//ppt.setSlideOrder(selectesdslide, 0);
				FileOutputStream out = new FileOutputStream(file);
				System.out.println("Yes");
				
			 	ppt.write(out);
				
			    out.close();
			    ppt = new HSLFSlideShow(new FileInputStream(file));
			    slides = ppt.getSlides();
			    System.out.println(slides.size());
			    int i = slides.size()-1;
				while(i>0)
				{
					ppt.removeSlide(i);
					i--;
				}
				out = new FileOutputStream(file);
				//Saving the changes to the presentation
				System.out.println(ppt.getSlides().size());
			    ppt.write(out);
			    out.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String getPreviewImage(String videoPath){
		FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper();
		String previewPath = ffmpegWrapper.getPreview(videoPath);
		return previewPath;
	}
}