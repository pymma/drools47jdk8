package org.drools.solver.examples.nqueens.swingui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.drools.solver.core.move.Move;
import org.drools.solver.examples.common.swingui.SolutionPanel;
import org.drools.solver.examples.nqueens.domain.NQueens;
import org.drools.solver.examples.nqueens.domain.Queen;
import org.drools.solver.examples.nqueens.solver.YChangeMove;

/**
 * @author Geoffrey De Smet
 */
public class NQueensPanel extends SolutionPanel {

    public NQueensPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    private NQueens getNQueens() {
        return (NQueens) solutionBusiness.getSolution();
    }

    public void resetPanel() {
        removeAll();
        NQueens nQueens = getNQueens();
        int n = nQueens.getN();
        List<Queen> queenList =  nQueens.getQueenList();
        setLayout(new GridLayout(n, n));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Queen queen = queenList.get(j);
                if (queen.getY() == i) {
                    add(new JButton(new QueenAction(queen)));
                } else {
                    JPanel panel = new JPanel();
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.DARK_GRAY),
                            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
                    Color background = (((i + j) % 2) == 0) ? Color.WHITE : Color.GRAY;
                    panel.setBackground(background);
                    add(panel);
                }
            }
        }
        validate();
    }

    private class QueenAction extends AbstractAction {

        private Queen queen;

        public QueenAction(Queen queen) {
            super("[" + queen.getId() + "]");
            this.queen = queen;
        }

        public void actionPerformed(ActionEvent e) {
            List<Integer> yList = getNQueens().createNList();
            JComboBox yListField = new JComboBox(yList.toArray());
            yListField.setSelectedItem(queen.getY());
            int result = JOptionPane.showConfirmDialog(NQueensPanel.this, yListField, "Select y",
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                int toY = (Integer) yListField.getSelectedItem();
                Move move = new YChangeMove(queen, toY);
                solutionBusiness.doMove(move);
                workflowFrame.updateScreen();
            }
        }

    }

}
