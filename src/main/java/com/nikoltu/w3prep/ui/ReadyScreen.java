package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.nikoltu.w3prep.GraphicsSet;
import com.nikoltu.w3prep.PackOptions;
import com.nikoltu.w3prep.Platform;

final class ReadyScreen extends JPanel {
	private static final long serialVersionUID = 1L;
	static final String LEGAL = "For use with your own legal copy only. Do not share the extracted files. "
			+ "This tool only reads your game install and never connects to the internet.";

	final JButton helpButton = Ui.quiet("Help");
	final JButton logButton = Ui.quiet("Log folder");
	final JButton chooseInstallButton = Ui.button("Choose…");
	final JButton changeOutputButton = Ui.button("Change…");
	final JButton advancedButton = Ui.link("Advanced", new Glyph(Glyph.Shape.CHEVRON_RIGHT, 12, Theme.GOLD_HI));
	final JButton createButton = Ui.primary("Create tablet package");

	private final JLabel title = Ui.title("Ready to make the tablet package");
	private final WrapText subtitle = Ui.sub("");
	private final RoundPanel installCard = Ui.card();
	private final JTextField installField = Ui.pathField("Game install folder");
	private final JPanel installStatus = Ui.column(8);
	private final RoundPanel packageCard = Ui.card();
	private final JTextField outputField = Ui.pathField("Package");
	private final WrapText summary = Ui.text("", Theme.SOFT);
	private final JPanel packageError = Ui.column(0);
	private final JPanel packageBody = Ui.column(10);
	private final WrapText packagePlaceholder = Ui.text("Shown after a game install is found.", Theme.DIM);
	private final JPanel advancedPanel;

	private final JRadioButton fullClassic = new JRadioButton("Full Classic", true);
	private final JRadioButton meleeOnly = new JRadioButton("Melee only");
	private final JCheckBox movies = new JCheckBox("Include movies");
	private final Map<GraphicsSet, JCheckBox> setBoxes = new EnumMap<>(GraphicsSet.class);
	private final Map<GraphicsSet, OptionRow> setRows = new EnumMap<>(GraphicsSet.class);
	private final JCheckBox desktopFolder = new JCheckBox("Also write a plain folder and warsmash-data.ini");
	private final OptionRow fullRow;
	private final OptionRow meleeRow;
	private final OptionRow moviesRow;

	ReadyScreen() {
		super(new BorderLayout());
		setOpaque(false);

		final JPanel header = new JPanel(new BorderLayout(16, 0));
		header.setOpaque(false);
		final JPanel titles = Ui.column(4);
		titles.add(this.title);
		titles.add(this.subtitle);
		header.add(titles, BorderLayout.CENTER);
		final JPanel links = Ui.row(2, this.helpButton, this.logButton);
		header.add(links, BorderLayout.EAST);

		this.installCard.add(Ui.heading("Game install folder"));
		this.installCard.add(Ui.fieldRow(this.installField, this.chooseInstallButton));
		this.installCard.add(this.installStatus);

		this.packageBody.add(Ui.fieldRow(this.outputField, this.changeOutputButton));
		this.packageBody.add(this.summary);
		this.packageBody.add(this.packageError);
		this.packageCard.add(Ui.heading("Package"));
		this.packageCard.add(this.packageBody);
		this.packageCard.add(this.packagePlaceholder);

		final ButtonGroup content = new ButtonGroup();
		content.add(this.fullClassic);
		content.add(this.meleeOnly);
		this.fullRow = new OptionRow(this.fullClassic, "Melee, campaigns, sound, music. What the tablet needs.", true);
		this.meleeRow = new OptionRow(this.meleeOnly, "Skips campaign maps and campaign assets.", true);
		this.moviesRow = new OptionRow(this.movies, "The tablet does not play them yet.", false);
		this.advancedPanel = Ui.quietCard();
		this.advancedPanel.setLayout(new ColumnLayout(14));
		final JPanel contentGroup = Ui.column(8);
		contentGroup.add(Ui.heading("Content"));
		contentGroup.add(this.fullRow);
		contentGroup.add(this.meleeRow);
		contentGroup.add(this.moviesRow);
		this.advancedPanel.add(contentGroup);

		final JPanel setGroup = Ui.column(8);
		setGroup.add(Ui.row(8, Ui.heading("Graphics sets"), Ui.tag("EXPERIMENTAL", Theme.WARN_BG, Theme.WARN)));
		for (final GraphicsSet set : GraphicsSet.values()) {
			final JCheckBox box = new JCheckBox(set.getLabel());
			final OptionRow row = new OptionRow(box, "", false);
			this.setBoxes.put(set, box);
			this.setRows.put(set, row);
			setGroup.add(row);
		}
		this.advancedPanel.add(setGroup);

		final JPanel desktopGroup = Ui.column(8);
		desktopGroup.add(Ui.heading("Desktop build"));
		desktopGroup.add(new OptionRow(this.desktopFolder, "For the PC build of the engine. Not needed for the tablet.",
				false));
		this.advancedPanel.add(desktopGroup);
		this.advancedPanel.setVisible(false);

		final JPanel body = new ScrollColumn(16);
		body.setBorder(BorderFactory.createEmptyBorder(22, 32, 12, 32));
		body.add(header);
		body.add(this.installCard);
		body.add(this.packageCard);
		body.add(Ui.row(0, this.advancedButton));
		body.add(this.advancedPanel);
		final JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		final JPanel footer = Ui.column(12);
		footer.setBorder(BorderFactory.createEmptyBorder(8, 32, 18, 32));
		final JPanel buttonRow = new JPanel(new BorderLayout());
		buttonRow.setOpaque(false);
		buttonRow.add(this.createButton, BorderLayout.EAST);
		footer.add(buttonRow);
		footer.add(Ui.rule());
		final WrapText legal = new WrapText(LEGAL, Theme.body(12f), Theme.MUTED);
		footer.add(legal);
		add(footer, BorderLayout.SOUTH);

		showChecking(null);
	}

	void setAdvancedOpen(final boolean open) {
		this.advancedPanel.setVisible(open);
		this.advancedButton.setIcon(new Glyph(open ? Glyph.Shape.CHEVRON_DOWN : Glyph.Shape.CHEVRON_RIGHT, 12,
				Theme.GOLD_HI));
		this.advancedButton.getAccessibleContext().setAccessibleDescription(open ? "expanded" : "collapsed");
		revalidate();
		repaint();
	}

	boolean isAdvancedOpen() {
		return this.advancedPanel.isVisible();
	}

	void showChecking(final Path folder) {
		this.title.setText("Ready to make the tablet package");
		this.subtitle.setText("Looking for your game install…");
		this.installField.setText(folder == null ? "" : folder.toString());
		this.installStatus.removeAll();
		this.installStatus.add(Ui.flowOf(Ui.chip("Checking…", Ui.ChipKind.IDLE)));
		this.installCard.setColors(Theme.PANEL, Theme.BRONZE);
		this.chooseInstallButton.putClientProperty("FlatLaf.style", "margin: 6,14,6,14");
		setPackageAvailable(false);
		this.createButton.setEnabled(false);
		refresh();
	}

	void showFound(final Path install, final String version, final String language) {
		this.title.setText("Ready to make the tablet package");
		this.subtitle.setText("Your game install was found. Nothing in it will be changed.");
		this.installField.setText(install.toString());
		this.installStatus.removeAll();
		this.installStatus.add(Ui.flowOf(Ui.chip("Version " + version, Ui.ChipKind.OK),
				Ui.chip("Storage readable", Ui.ChipKind.OK), Ui.chip("Classic files present", Ui.ChipKind.OK),
				Ui.chip("Language " + language, Ui.ChipKind.OK)));
		this.installCard.setColors(Theme.PANEL, Theme.BRONZE);
		this.chooseInstallButton.putClientProperty("FlatLaf.style", "margin: 6,14,6,14");
		setPackageAvailable(true);
		this.createButton.setEnabled(true);
		refresh();
	}

	void showInstallProblem(final Path tried, final String message, final boolean nothingFound) {
		this.title.setText(nothingFound ? "Where is the game installed?" : "Ready to make the tablet package");
		this.subtitle.setText(nothingFound ? "No install was found in the usual places. Pick its folder once; it is "
				+ "remembered." : "This folder can't be used. Pick another one.");
		this.installField.setText(tried == null ? "" : tried.toString());
		this.installStatus.removeAll();
		if (message != null) {
			final RoundPanel error = new RoundPanel(new BorderLayout(8, 0), Theme.ERR_BG, null, 10, 8, 12);
			final JLabel icon = new JLabel(new Glyph(Glyph.Shape.CROSS, 13, Theme.ERR));
			icon.setVerticalAlignment(JLabel.TOP);
			error.add(icon, BorderLayout.WEST);
			error.add(Ui.text(message, Theme.ERR), BorderLayout.CENTER);
			this.installStatus.add(error);
		}
		this.installStatus.add(new WrapText("Tip: in the game launcher, open the game's settings and use \"Show in "
				+ Platform.fileManagerName() + "\". That is the folder to pick.", Theme.body(12.5f), Theme.MUTED));
		this.installCard.setColors(Theme.PANEL, Theme.ERR_EDGE);
		this.chooseInstallButton.putClientProperty("FlatLaf.style",
				"margin: 6,14,6,14; background: #2B2416; foreground: #E8C76A; borderColor: #C9A24A");
		setPackageAvailable(false);
		this.createButton.setEnabled(false);
		refresh();
	}

	private void setPackageAvailable(final boolean available) {
		this.packageBody.setVisible(available);
		this.packagePlaceholder.setVisible(!available);
		this.packageCard.setColors(available ? Theme.PANEL : Theme.PANEL_2, available ? Theme.BRONZE : Theme.RULE);
	}

	void setOutput(final Path packageFile) {
		this.outputField.setText(packageFile.toString());
	}

	void setSummary(final String text) {
		this.summary.setText(text);
		refresh();
	}

	void setPackageError(final String message) {
		this.packageError.removeAll();
		if (message != null) {
			this.packageError.add(Ui.notice(message, Theme.ERR_BG, Theme.ERR));
		}
		refresh();
	}

	void setSizes(final String full, final String melee, final String moviesSize) {
		this.fullRow.setSize(full);
		this.meleeRow.setSize(melee);
		this.moviesRow.setSize(moviesSize);
	}

	void setSetAvailability(final GraphicsSet set, final boolean available, final String size) {
		final OptionRow row = this.setRows.get(set);
		row.setEnabled(available);
		row.setSize(available ? size : "–");
		row.setDescription(available
				? "Separate package " + set.getPackageName() + ". The engine does not use it yet."
				: "Not on this install.");
	}

	PackOptions readOptions() {
		final Set<GraphicsSet> sets = EnumSet.noneOf(GraphicsSet.class);
		for (final Map.Entry<GraphicsSet, JCheckBox> box : this.setBoxes.entrySet()) {
			if (box.getValue().isSelected() && box.getValue().isEnabled()) {
				sets.add(box.getKey());
			}
		}
		return new PackOptions(this.meleeOnly.isSelected() ? PackOptions.Content.MELEE_ONLY
				: PackOptions.Content.FULL_CLASSIC, this.movies.isSelected(), sets, this.desktopFolder.isSelected());
	}

	void onOptionsChanged(final Runnable listener) {
		this.fullClassic.addActionListener(e -> listener.run());
		this.meleeOnly.addActionListener(e -> listener.run());
		this.movies.addActionListener(e -> listener.run());
		this.desktopFolder.addActionListener(e -> listener.run());
		for (final JCheckBox box : this.setBoxes.values()) {
			box.addActionListener(e -> listener.run());
		}
	}

	private void refresh() {
		revalidate();
		repaint();
	}
}
