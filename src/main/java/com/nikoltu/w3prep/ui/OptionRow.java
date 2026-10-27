package com.nikoltu.w3prep.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class OptionRow extends RoundPanel {
	private static final long serialVersionUID = 1L;
	private final AbstractButton button;
	private final WrapText description;
	private final JLabel size;
	private final boolean highlightSelection;

	OptionRow(final AbstractButton button, final String descriptionText, final boolean highlightSelection) {
		super(new BorderLayout(10, 0), null, Theme.RULE, 10, 9, 12);
		this.button = button;
		this.highlightSelection = highlightSelection;
		button.setFont(Theme.bold(13f));
		button.setOpaque(false);
		button.setBorder(BorderFactory.createEmptyBorder());
		this.description = new WrapText(descriptionText, Theme.body(12.5f), Theme.MUTED);
		this.size = Ui.label("", Theme.body(12.5f), Theme.SOFT);

		final JPanel text = Ui.column(2);
		text.add(button);
		text.add(this.description);
		add(text, BorderLayout.CENTER);
		add(this.size, BorderLayout.EAST);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (OptionRow.this.button.isEnabled()) {
					OptionRow.this.button.doClick();
				}
			}
		});
		button.addItemListener(e -> refresh());
		refresh();
	}

	void setSize(final String text) {
		this.size.setText(text);
	}

	void setDescription(final String text) {
		this.description.setText(text);
		revalidate();
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		this.button.setEnabled(enabled);
		if (!enabled) {
			this.button.setSelected(false);
		}
		this.description.setForeground(enabled ? Theme.MUTED : Theme.DIM);
		this.size.setForeground(enabled ? Theme.SOFT : Theme.DIM);
		refresh();
	}

	private void refresh() {
		if (this.highlightSelection && this.button.isSelected()) {
			setColors(Theme.GOLD_WASH, Theme.GOLD);
		} else {
			setColors(null, Theme.RULE);
		}
	}
}
