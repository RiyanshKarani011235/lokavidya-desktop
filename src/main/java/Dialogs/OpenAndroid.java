package Dialogs;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;

import com.iitb.lokavidya.core.operations.ProjectService;
import com.iitb.lokavidya.core.utils.GeneralUtils;

import gui.Call;

public class OpenAndroid {
	public JFrame frame;
	public String pathDef;
	public static String path;
	private JButton btnNewButton_1;
	private JTextField textField_2;
	public JLabel lblNewLabel;
	
	public JProgressBar progressBar;
	public JPanel innerPanel;
	private JButton btnCancel;
	private JLabel lblNewLabel1;

	public static void copyFile( File from, File to ) {
		 //  Files.delete(to.toPath());
	    	
	    	try {
				Files.copy( from.toPath(), to.toPath() );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Call.workspace.cancelled=false;
					OpenAndroid window = new OpenAndroid();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	public OpenAndroid() {
		initialize();
	}
	
	
	class ProgressDialog extends JPanel
	implements ActionListener, 
	PropertyChangeListener{
		
		 /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		 public Task task;
		
		 
		 class Task extends SwingWorker<Void, Void> {

			@Override
			protected Void doInBackground() throws Exception {
				int progress=0;
				 Call.workspace.startOperation();
				 setProgress(0);
				 setProgress(10);
				 
				ProjectService.importAndroidProject(Call.workspace.currentProject.getProjectJsonPath(), path);
				setProgress(80);
				if(!Call.workspace.cancelled)
				{
					Call.workspace.repopulateProject();
					setProgress(100);
					Thread.sleep(1000);
					Call.workspace.endOperation();
				}
				else
				{
					lblNewLabel1.setText("Cancelling import");
			 		setProgress(50);
			 		Call.workspace.cancelOperation();
			 		Thread.sleep(1000);
				}
				
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				frame.dispose();
				return null;
				 
			}
		 }


		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName()) {
	            int progress = (Integer) evt.getNewValue();
	            progressBar.setIndeterminate(false);
	            progressBar.setValue(progress);
			}
			
		}


		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
		ProgressDialog() {

			innerPanel.setVisible(true);
			
	        System.out.println("Progress dialog created");
	        task = new Task();
	        task.addPropertyChangeListener(this);
	        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	        task.execute();
		}
	}
	
	public void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 542, 280);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		SpringLayout springLayout = new SpringLayout();
		frame.getContentPane().setLayout(springLayout);
		lblNewLabel = new JLabel("Open Android Project");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 18));
		frame.getContentPane().add(lblNewLabel);
		
		textField_2 = new JTextField();
		springLayout.putConstraint(SpringLayout.EAST, lblNewLabel, -6, SpringLayout.WEST, textField_2);
		springLayout.putConstraint(SpringLayout.NORTH, textField_2, 73, SpringLayout.NORTH, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.WEST, textField_2, 212, SpringLayout.WEST, frame.getContentPane());

		progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true); 
        progressBar.setIndeterminate(true);
      
        innerPanel = new JPanel();
        springLayout.putConstraint(SpringLayout.WEST, innerPanel, 0, SpringLayout.WEST, textField_2);
        innerPanel.setLayout(new BorderLayout(0, 0));
        innerPanel.add(progressBar);
        innerPanel.setSize(400, 30);
        innerPanel.setVisible(false);
        innerPanel.setOpaque(true);
        
        
        lblNewLabel1 = new JLabel("Importing project. Please wait....");
        innerPanel.add(lblNewLabel1, BorderLayout.SOUTH);
        //innerPanel.setVisible(false);
		frame.getContentPane().add(innerPanel);
		
		
		textField_2.setColumns(10);
		String Os = System.getProperty("os.name");
		pathDef=GeneralUtils.getDocumentsPath();
		textField_2.setText(pathDef);
		frame.getContentPane().add(textField_2);
		
		JButton btnNewButton_2 = new JButton(" ... ");
		springLayout.putConstraint(SpringLayout.NORTH, lblNewLabel, 0, SpringLayout.NORTH, btnNewButton_2);
		springLayout.putConstraint(SpringLayout.NORTH, btnNewButton_2, 74, SpringLayout.NORTH, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.EAST, textField_2, -3, SpringLayout.WEST, btnNewButton_2);
		springLayout.putConstraint(SpringLayout.EAST, btnNewButton_2, -10, SpringLayout.EAST, frame.getContentPane());
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				path=new DirectoryChooser(pathDef,"zip").selectedfile;

				textField_2.setText(path);
			}
		});
		frame.getContentPane().add(btnNewButton_2);
		
		
		btnNewButton_1 = new JButton("Import");
		springLayout.putConstraint(SpringLayout.NORTH, innerPanel, 0, SpringLayout.NORTH, btnNewButton_1);
		springLayout.putConstraint(SpringLayout.WEST, btnNewButton_1, 27, SpringLayout.WEST, frame.getContentPane());
		springLayout.putConstraint(SpringLayout.SOUTH, btnNewButton_1, -28, SpringLayout.SOUTH, frame.getContentPane());
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(textField_2.getText().equals(""))
				{
					System.out.println("Path null");
					JOptionPane.showMessageDialog(null, "Enter the project location", "", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					path=textField_2.getText();
					new ProgressDialog();
					//new ConverttoDesktop(path);
					//System.out.println(projName);
					

				}
			}
		});
		btnNewButton_1.setFont(new Font("Tahoma", Font.PLAIN, 14));
		
		frame.getContentPane().add(btnNewButton_1);
		
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Call.workspace.cancelled=true;
			}
		});
		springLayout.putConstraint(SpringLayout.NORTH, btnCancel, 0, SpringLayout.NORTH, innerPanel);
		springLayout.putConstraint(SpringLayout.WEST, btnCancel, 6, SpringLayout.EAST, btnNewButton_1);
		btnCancel.setFont(new Font("Tahoma", Font.PLAIN, 14));
		// disable cancel button - ironstein - 22-11-16
//		frame.getContentPane().add(btnCancel);
		
	}

}
