package org.openmolecules.render;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.util.ColorHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;

public class MoleculeRaytraceDialog extends JDialog implements ActionListener {
	@Serial
	private static final long serialVersionUID = 20150604L;

	private static final String[] SIZE_OPTIONS = {
			"640 x 480", 
			"1024 x 768",
			"1600 x 1200",
			"1920 x 1080",
			"2560 x 1600",
			"4000 x 3000" };

	private final JComboBox<String> mComboboxSize,mComboboxMode,mComboboxAtomMaterial,mComboboxBondMaterial;
	private final JCheckBox mCheckboxFillImage,mCheckboxUseBackground,mCheckboxUseFloor,mCheckboxOptimizeRotation,mCheckboxShinyFloor;
	private final ColorPanel mBackgroundColorPanel,mFloorColorPanel;
	private final JButton mBackgroundButton,mFloorButton;
	private final Conformer mConformer;
	private final float mCameraDistance,mCameraX,mCameraZ,mFieldOfView;

	/**
	 * Creates a raytrace dialog that will render an image with the default parameters
	 * for camera distance (12.5) and field of view (40f). 
	 * @param parent
	 * @param conformer
	 */
	public MoleculeRaytraceDialog(Frame parent, Conformer conformer) {
		this(parent, conformer, SunflowPrimitiveBuilder.DEFAULT_CAMERA_DISTANCE,
								SunflowPrimitiveBuilder.DEFAULT_CAMERA_X,
								SunflowPrimitiveBuilder.DEFAULT_CAMERA_Z,
								SunflowPrimitiveBuilder.DEFAULT_FIELD_OF_VIEW);
		}

	/**
	 * 
	 * @param parent
	 * @param conformer
	 * @param cameraDistance
	 * @param fieldOfView
	 */
	public MoleculeRaytraceDialog(Frame parent, Conformer conformer, float cameraDistance, float cameraX, float cameraZ, float fieldOfView) {
		super(parent, "Create Photo-Realistic Image", true);

		mConformer = conformer;
		mCameraDistance = cameraDistance;
		mCameraX = cameraX;
		mCameraZ = cameraZ;
		mFieldOfView = fieldOfView;

		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							16, TableLayout.PREFERRED, 8} };

		getContentPane().setLayout(new TableLayout(size));

		getContentPane().add(new JLabel("Image size:"), "1,1");
		mComboboxSize = new JComboBox<>(SIZE_OPTIONS);
		mComboboxSize.setSelectedIndex(0);
		getContentPane().add(mComboboxSize, "3,1");

		getContentPane().add(new JLabel("Render mode:"), "1,3");
		mComboboxMode = new JComboBox<>(MoleculeArchitect.MODE_TEXT);
		mComboboxMode.setSelectedIndex(MoleculeArchitect.CONSTRUCTION_MODE_DEFAULT);
		getContentPane().add(mComboboxMode, "3,3");

		getContentPane().add(new JLabel("Atom material:"), "1,5");
		mComboboxAtomMaterial = new JComboBox<>(SunflowPrimitiveBuilder.MATERIAL_TEXT);
		mComboboxAtomMaterial.setSelectedIndex(SunflowMoleculeBuilder.DEFAULT_ATOM_MATERIAL);
		getContentPane().add(mComboboxAtomMaterial, "3,5");

		getContentPane().add(new JLabel("Bond material:"), "1,7");
		mComboboxBondMaterial = new JComboBox<>(SunflowPrimitiveBuilder.MATERIAL_TEXT);
		mComboboxBondMaterial.setSelectedIndex(SunflowMoleculeBuilder.DEFAULT_BOND_MATERIAL);
		getContentPane().add(mComboboxBondMaterial, "3,7");

		double[][] sizeColorPanel = { {TableLayout.FILL, 64, 12, TableLayout.PREFERRED}, {TableLayout.PREFERRED} };

		mCheckboxUseBackground = new JCheckBox("Use opaque background", SunflowPrimitiveBuilder.DEFAULT_USE_BACKGROUND);
		mCheckboxUseBackground.addActionListener(this);
		getContentPane().add(mCheckboxUseBackground, "1,9,3,9");

		JPanel backgroundPanel = new JPanel();
		backgroundPanel.setLayout(new TableLayout(sizeColorPanel));
		mBackgroundButton = new JButton("Change");
		mBackgroundButton.setActionCommand("background");
		mBackgroundColorPanel = addColorChooser(mBackgroundButton, 0, SunflowPrimitiveBuilder.DEFAULT_BACKGROUND, backgroundPanel);
		getContentPane().add(backgroundPanel, "1,11,3,11");

		mCheckboxUseFloor = new JCheckBox("Use floor to cast shadows", SunflowPrimitiveBuilder.DEFAULT_USE_FLOOR);
		mCheckboxUseFloor.addActionListener(this);
		getContentPane().add(mCheckboxUseFloor, "1,13,3,13");

		JPanel floorPanel = new JPanel();
		floorPanel.setLayout(new TableLayout(sizeColorPanel));
		mFloorButton = new JButton("Change");
		mFloorButton.setActionCommand("floor");
		mFloorColorPanel = addColorChooser(mFloorButton, 0, SunflowPrimitiveBuilder.DEFAULT_FLOOR_COLOR, floorPanel);
		getContentPane().add(floorPanel, "1,15,3,15");

		mCheckboxShinyFloor = new JCheckBox("Glossy floor", SunflowPrimitiveBuilder.DEFAULT_GLOSSY_FLOOR);
		mCheckboxShinyFloor.setHorizontalAlignment(JCheckBox.CENTER);
		getContentPane().add(mCheckboxShinyFloor, "1,17,3,17");

		mCheckboxFillImage = new JCheckBox("Move and zoom to fill image", true);
		getContentPane().add(mCheckboxFillImage, "1,19,3,19");

		mCheckboxOptimizeRotation = new JCheckBox("Rotate for best view", true);
		getContentPane().add(mCheckboxOptimizeRotation, "1,21,3,21");

		enableItems();

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2, 8, 8));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		buttonPanel.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		buttonPanel.add(bok);
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(buttonPanel, BorderLayout.EAST);
		getContentPane().add(bottomPanel, "1,23,3,23");

		getRootPane().setDefaultButton(bok);

		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		}

	private ColorPanel addColorChooser(JButton button, int position, Color color, JPanel backgroundPanel) {
		ColorPanel colorPanel = new ColorPanel(color);
		backgroundPanel.add(colorPanel, "1,"+position);
		button.addActionListener(this);
		backgroundPanel.add(button, "3,"+position);
		return colorPanel;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckboxUseBackground
		 || e.getSource() == mCheckboxUseFloor) {
			enableItems();
			return;
			}
		if (e.getActionCommand().equals("background")) {
			Color newColor = JColorChooser.showDialog(this, "Select Background Color", mBackgroundColorPanel.getColor());
			if (newColor == null || newColor.equals(mBackgroundColorPanel.getColor()))
				return;
			mBackgroundColorPanel.setColor(newColor);
			return;
			}
		if (e.getActionCommand().equals("floor")) {
			Color newColor = JColorChooser.showDialog(this, "Select Floor Color", mFloorColorPanel.getColor());
			if (newColor == null || newColor.equals(mFloorColorPanel.getColor()))
				return;
			mFloorColorPanel.setColor(newColor);
			return;
			}
		if (e.getActionCommand().equals("OK")) {
			renderMolecule((String)mComboboxSize.getSelectedItem(), mComboboxMode.getSelectedIndex(),
					mComboboxAtomMaterial.getSelectedIndex(), mComboboxBondMaterial.getSelectedIndex(),
					mCheckboxUseBackground.isSelected() ? mBackgroundColorPanel.getColor() : null,
					mCheckboxUseFloor.isSelected() ? mFloorColorPanel.getColor() : null, mCheckboxShinyFloor.isSelected(),
					mCheckboxFillImage.isSelected(), mCheckboxOptimizeRotation.isSelected());
			}

		setVisible(false);
		dispose();
		}

	private void enableItems() {
		mBackgroundColorPanel.setEnabled(mCheckboxUseBackground.isSelected());
		mBackgroundButton.setEnabled(mCheckboxUseBackground.isSelected());
		mFloorColorPanel.setEnabled(mCheckboxUseFloor.isSelected());
		mFloorButton.setEnabled(mCheckboxUseFloor.isSelected());
		mCheckboxShinyFloor.setEnabled(mCheckboxUseFloor.isSelected());
		}

	private void renderMolecule(final String sizeOption, final int renderMode, final int atomMaterial, final int bondMaterial,
	                            final Color background, final Color floorColor, final boolean glossyFloor,
	                            final boolean fillImage, final boolean optimizeRotation) {
		final boolean moveToCenter = fillImage;
		final boolean zoomToOptimum = fillImage;
		new Thread(() -> {
			int i = sizeOption.indexOf(" x ");
			int width = Integer.parseInt(sizeOption.substring(0, i));
			int height = Integer.parseInt(sizeOption.substring(i+3));

			if (moveToCenter) {
				Coordinates cog = new Coordinates();	// center of gravity
				for (int atom=0; atom<mConformer.getSize(); atom++)
					cog.add(mConformer.getCoordinates(atom));
				cog.scale(1.0 / mConformer.getSize());

				for (int atom=0; atom<mConformer.getSize(); atom++)
					mConformer.getCoordinates(atom).sub(cog);
				}

			SunflowMoleculeBuilder mr = fillImage ? new SunflowMoleculeBuilder()
					: new SunflowMoleculeBuilder(mCameraDistance, mCameraX, mCameraZ, mFieldOfView);
			mr.setRenderMode(renderMode);
			mr.setAtomMaterial(atomMaterial);
			mr.setBondMaterial(bondMaterial);
			mr.setBackgroundColor(background);
			mr.setFloorColor(floorColor);
			mr.setGlossyFloor(glossyFloor);
			mr.initializeScene(width, height);
			mr.drawMolecule(mConformer, optimizeRotation, zoomToOptimum, -1f);
			mr.finalizeScene(-1);
//				mr.render("/home/thomas/sunflowTest.png");
			mr.render();
			}).start();
		}

	static class ColorPanel extends JPanel {
		@Serial
		private static final long serialVersionUID = 0x20110427;
		private final Color mOriginalColor;
		private Color mColor;

		public ColorPanel(Color c) {
			super();
			mOriginalColor = mColor = c;
			}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(isEnabled() ? mColor
					: ColorHelper.intermediateColor(mColor, UIManager.getColor("Panel.background"), 0.7f));
			g.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 2, 2);
			g.setColor(UIManager.getColor("Panel.background").darker());
			g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
			}

		public Color getColor() {
			return mColor;
		}

		public void setColor(Color c) {
			mColor = c;
			repaint();
			}

		public Color getOriginalColor() {
			return mOriginalColor;
			}
		}
	}
