package eclihx.ui.internal.ui;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import eclihx.ui.internal.ui.views.PackageExplorerView;

/**
 * Class for building haXe perspective
 */
public class HaxePerspectiveFactory implements IPerspectiveFactory {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();

		// Top left
		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.25, editorArea);
		topLeft.addView(PackageExplorerView.PACKAGE_EXPLORER_ID);
		topLeft.addPlaceholder(IPageLayout.ID_BOOKMARKS);

		// Bottom right
		IFolderLayout bottomRight = layout.createFolder(
				"bottomRight", IPageLayout.BOTTOM, (float) 0.65, editorArea);
		bottomRight.addView(IPageLayout.ID_TASK_LIST);
		bottomRight.addView(IPageLayout.ID_PROBLEM_VIEW);
		
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		
		layout.addShowViewShortcut(PackageExplorerView.PACKAGE_EXPLORER_ID);
		
		layout.addNewWizardShortcut(
				"eclihx.ui.internal.ui.wizards.HaxeProjectWizard");
		
		layout.addNewWizardShortcut(
				"eclihx.ui.internal.ui.wizards.HaxeFileWizard");
		
		layout.addNewWizardShortcut(
				"eclihx.ui.internal.ui.wizards.HaxeBuildFileWizard");
	}
}
