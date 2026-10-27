package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.nikoltu.w3prep.ReadFailure;

final class FailedScreen extends JPanel {
	private static final long serialVersionUID = 1L;
	final JButton copyButton = Ui.button("Copy details");
	final JButton backButton = Ui.button("Back");
	final JButton retryButton = Ui.primary("Try again");
	final JButton skipButton = Ui.button("Make it without these files");

	private final JLabel title = Ui.title("");
	private final WrapText message = Ui.sub("");
	private final RoundPanel listCard = Ui.card();
	private final WrapText advice = Ui.text("", Theme.SOFT);
	private String details = "";

	FailedScreen() {
		super(new BorderLayout());
		setOpaque(false);
		final JPanel body = Ui.column(18);
		body.setBorder(BorderFactory.createEmptyBorder(22, 32, 20, 32));

		final JPanel titles = Ui.column(2);
		titles.add(this.title);
		titles.add(this.message);
		final JPanel header = new JPanel(new BorderLayout(14, 0));
		header.setOpaque(false);
		header.add(new JLabel(new Glyph(Glyph.Shape.EXCLAMATION, 40, Theme.WARN, Theme.WARN_BG)), BorderLayout.WEST);
		header.add(titles, BorderLayout.CENTER);
		body.add(header);
		body.add(this.listCard);

		final RoundPanel adviceCard = Ui.quietCard();
		adviceCard.add(Ui.heading("What to do"));
		adviceCard.add(this.advice);
		adviceCard.add(Ui.label("Nothing was written. A package you made earlier is unchanged.", Theme.body(12.5f),
				Theme.MUTED));
		body.add(adviceCard);

		final JPanel spacer = new JPanel();
		spacer.setOpaque(false);
		body.add(spacer, ColumnLayout.FILL);
		final JPanel bottom = new JPanel(new BorderLayout());
		bottom.setOpaque(false);
		bottom.add(this.copyButton, BorderLayout.WEST);
		bottom.add(Ui.row(10, this.backButton, this.skipButton, this.retryButton), BorderLayout.EAST);
		body.add(bottom);
		add(body, BorderLayout.CENTER);
	}

	void showReadFailures(final List<ReadFailure> failures) {
		this.title.setText("The package was not made");
		this.message.setText(failures.size() + (failures.size() == 1 ? " file" : " files")
				+ " could not be read. A package with missing files is only made if you ask for it.");
		this.listCard.removeAll();
		this.listCard.add(Ui.heading("Files that could not be read"));
		final StringBuilder all = new StringBuilder();
		for (int i = 0; i < failures.size(); i++) {
			all.append(failures.get(i)).append(System.lineSeparator());
			if (i < 3) {
				this.listCard.add(new WrapText(failures.get(i).toString(), Theme.mono(), Theme.SOFT));
			}
		}
		if (failures.size() > 3) {
			this.listCard.add(Ui.label("and " + (failures.size() - 3) + " more in the log", Theme.body(12.5f),
					Theme.MUTED));
		}
		this.listCard.setVisible(true);
		this.advice.setText("Usually the game is still updating, or the install is damaged. Let the game launcher "
				+ "finish updating, or run its \"Scan and Repair\". Then try again. If you don't need these files, "
				+ "you can make the package without them; they are listed in its manifest.");
		this.skipButton.setVisible(true);
		this.details = all.toString();
		refresh();
	}

	void showProblem(final String titleText, final String messageText, final String adviceText,
			final String detailText) {
		this.title.setText(titleText);
		this.message.setText(messageText);
		this.listCard.removeAll();
		this.listCard.setVisible(false);
		this.skipButton.setVisible(false);
		this.advice.setText(adviceText);
		this.details = detailText == null ? messageText : detailText;
		refresh();
	}

	String getDetails() {
		return this.details;
	}

	private void refresh() {
		revalidate();
		repaint();
	}
}
