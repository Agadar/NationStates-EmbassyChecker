package com.github.agadar.embassychecker;

import com.github.agadar.embassychecker.event.RegionRetrievedEvent;
import com.github.agadar.embassychecker.event.RegionRetrievingStartedEvent;
import com.github.agadar.embassychecker.event.RegionEventsListener;
import com.github.agadar.nationstates.DefaultNationStatesImpl;
import com.github.agadar.nationstates.NationStates;
import com.github.agadar.nationstates.enumerator.RegionTag;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * EmbassyCheckController class for this program.
 *
 * @author Agadar <https://github.com/Agadar/>
 */
public final class EmbassyCheckController implements RegionEventsListener {

    private final EmbassyCheckForm form;
    private final NationStates nationStates;

    /**
     * The user agent for this program.
     */
    private final static String USER_AGENT = "Agadar's Embassy Checker "
            + "(https://github.com/Agadar/NationStates-EmbassyChecker)";

    /**
     * Constructor, taking a form to communicate with. Also sets the User Agent.
     *
     * @param form the form to communicate with
     */
    public EmbassyCheckController(EmbassyCheckForm form) {
        nationStates = new DefaultNationStatesImpl(USER_AGENT);
        this.form = form;
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
            RegionTag[] tagsToCheck) {
        // Disable the GUI components, clear the textarea, reset the progress bar.
        setComponentsEnabled(false);
        form.TxtAreaReport.setText("");
        EmbassyCheckQuery query;

        // Build a new query according to the supplied parameters.
        try {
            query = new EmbassyCheckQuery(nationStates, mainRegionName).addListeners(this);

            if (checkRmbActivity) {
                query = query.rmbActivity(maxDaysSinceLastRmbMsg);
            }
            if (checkRegionFounded) {
                query = query.minimumAge(minDaysSinceFounded);
            }
            if (checkRegionTags) {
                query = query.regionTags(tagsToCheck);
            }
        } catch (IllegalArgumentException ex) {
            // If an IllegalArgumentException is thrown, show it in a dialog,
            // re-enable the tools, and return.
            JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(),
                    "An Error Occured", JOptionPane.ERROR_MESSAGE);
            setComponentsEnabled(true);
            return;
        }

        // Create a new thread, and execute the query within.
        final EmbassyCheckQuery fquery = query;
        new Thread(() -> {
            try {
                // Execure query.
                String report = fquery.execute();

                // Print the report to the text area.
                SwingUtilities.invokeLater(() -> {
                    form.TxtAreaReport.setText(report);
                });
            } catch (IllegalArgumentException ex) {
                // If an IllegalArgumentException is thrown, show it in a dialog.
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(),
                            "An Error Occured", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                // Re-enable the GUI components.
                SwingUtilities.invokeLater(() -> {
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
    public void setComponentsEnabled(boolean enabled) {
        form.TxtFieldRegionName.setEditable(enabled);
        form.TxtAreaReport.setEditable(enabled);

        form.ChkbxRegionAge.setEnabled(enabled);
        form.ChkbxRmbActivity.setEnabled(enabled);
        form.ChkbxTags.setEnabled(enabled);

        form.BtnStart.setEnabled(enabled && (form.ChkbxRegionAge.isSelected()
                || form.ChkbxRmbActivity.isSelected() || form.ChkbxTags.isSelected()));

        form.SpinnerRegionAge.setEnabled(enabled && form.ChkbxRegionAge.isSelected());
        form.SpinnerRmbActivity.setEnabled(enabled && form.ChkbxRmbActivity.isSelected());

        form.TxtFieldTags.setEditable(enabled && form.ChkbxTags.isSelected());
    }

    @Override
    public void handleRetrievingStarted(RegionRetrievingStartedEvent event) {
        SwingUtilities.invokeLater(() -> {
            form.ProgressBar.setValue(0);
            form.ProgressBar.setMaximum(event.regionsToRetrieve - 1);
        });
    }

    @Override
    public void handleRegionRetrieved(RegionRetrievedEvent event) {
        SwingUtilities.invokeLater(() -> {
            form.ProgressBar.setValue(event.positionInQuery);
        });
    }
}
