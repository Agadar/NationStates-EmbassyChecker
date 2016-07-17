package com.github.agadar.embassychecker;

import com.github.agadar.nsapi.NSAPI;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * EmbassyCheckController class for this program.
 * 
 * @author Agadar <https://github.com/Agadar/>
 */
public final class EmbassyCheckController
{   
    /** The form to send updates to. */
    private final EmbassyCheckForm Form;
    
    /** The user agent for this program. */
    private final static String UserAgent = "Agadar's Embassy Checker "
            + "(https://github.com/Agadar/NationStates-EmbassyChecker)";
    
    /**
     * Constructor, taking a form to communicate with. Also sets the User Agent.
     * 
     * @param form the form to communicate with
     */
    public EmbassyCheckController(EmbassyCheckForm form)
    {
        // Set User Agent.
        NSAPI.setUserAgent(UserAgent);
        
        // Set form.
        Form = form;
    }
    
    /**
     * Starts building a report. Called from the GUI.
     * 
     * @param mainRegionName
     * @param checkRmbActivity
     * @param maxDaysSinceLastRmbMsg
     * @param checkRegionFounded
     * @param minDaysSinceFounded
     * @param checkRegionTags
     * @param tagsToCheck 
     */
    public void startReporting(String mainRegionName, boolean checkRmbActivity, 
            int maxDaysSinceLastRmbMsg, boolean checkRegionFounded,
            int minDaysSinceFounded, boolean checkRegionTags,
            String[] tagsToCheck)
    {
        // Disable the GUI components, clear the textarea, reset the progress bar.
        setComponentsEnabled(false);
        Form.TxtAreaReport.setText("");
        EmbassyCheckQuery query;
        
        // Build a new query according to the supplied parameters.
        try
        {
            query = new EmbassyCheckQuery(mainRegionName, this);
            
            if (checkRmbActivity)
            {
                query = query.rmbActivity(maxDaysSinceLastRmbMsg);
            }
            if (checkRegionFounded)
            {
                query = query.minimumAge(minDaysSinceFounded);
            }
            if (checkRegionTags)
            {
                query = query.regionTags(tagsToCheck);
            }
        }
        catch (IllegalArgumentException ex)
        {
            // If an IllegalArgumentException is thrown, show it in a dialog,
            // re-enable the tools, and return.
            JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), 
                "An Error Occured", JOptionPane.ERROR_MESSAGE);
            setComponentsEnabled(true);
            return;
        }
        
        // Create a new thread, and execute the query within.
        final EmbassyCheckQuery fquery = query;       
        new Thread(() ->
        {
            try
            {
                // Execure query.
                String report = fquery.execute();
                
                // Print the report to the text area.
                SwingUtilities.invokeLater(() ->
                {
                    Form.TxtAreaReport.setText(report);
                });
            } 
            catch (IllegalArgumentException ex)
            {
                // If an IllegalArgumentException is thrown, show it in a dialog.
                SwingUtilities.invokeLater(() ->
                {
                    JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), 
                        "An Error Occured", JOptionPane.ERROR_MESSAGE);
                });               
            }
            finally
            {
                // Re-enable the GUI components.
                SwingUtilities.invokeLater(() ->
                {
                    setComponentsEnabled(true);
                }); 
            }
        }).start();
    }
    
    /**
     * Enables or disables all GUI components.
     * 
     * @param enabled 
     */
    public void setComponentsEnabled(boolean enabled)
    { 
        Form.TxtFieldRegionName.setEditable(enabled);
        Form.TxtAreaReport.setEditable(enabled);
        
        Form.ChkbxRegionAge.setEnabled(enabled);
        Form.ChkbxRmbActivity.setEnabled(enabled);
        Form.ChkbxTags.setEnabled(enabled);
        
        Form.BtnStart.setEnabled(enabled && (Form.ChkbxRegionAge.isSelected() 
            || Form.ChkbxRmbActivity.isSelected() || Form.ChkbxTags.isSelected()));
        
        Form.SpinnerRegionAge.setEnabled(enabled && Form.ChkbxRegionAge.isSelected());
        Form.SpinnerRmbActivity.setEnabled(enabled && Form.ChkbxRmbActivity.isSelected());
        
        Form.TxtFieldTags.setEditable(enabled && Form.ChkbxTags.isSelected());
    }
    
    /**
     * Resets the progress bar via SwingUtilities.invokeLater(...) using the
     * given value as maximum. Called via the currently executing query.
     * 
     * @param maximum maximum value for the progress bar
     */
    public void resetProgressBar(int maximum)
    {
        SwingUtilities.invokeLater(() ->
        {
            Form.ProgressBar.setValue(0);
            Form.ProgressBar.setMaximum(maximum);
        });
    }
    
    /**
     * Increments the progress bar via SwingUtilities.invokeLater(...) by 1.
     * Called via the currently executing query.
     */
    public void incrProgressBar()
    {
        SwingUtilities.invokeLater(() ->
        {
            Form.ProgressBar.setValue(Form.ProgressBar.getValue() + 1);
        });
    }
}
