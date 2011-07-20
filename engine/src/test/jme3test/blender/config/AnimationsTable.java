package jme3test.blender.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Table for displaying and managing animations to import.
 * @author Marcin Roguski (Kaelthas)
 */
public class AnimationsTable extends JTable {
	private static final long serialVersionUID = 1978778634957586330L;

	/**
	 * Constructor. Creates default model. Applies basic settings.
	 */
	public AnimationsTable() {
		super(new AnimationsTableModel(new Object[] {null, "Object name", "Animation name", "Start", "Stop"}));
		this.getTableHeader().setReorderingAllowed(false);
		this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		CellRenderer cellRenderer = new CellRenderer();
		this.setDefaultRenderer(Object.class, cellRenderer);
		this.getModel().addTableModelListener(cellRenderer);
		this.getColumnModel().getColumn(0).setCellEditor(new RadioButtonCellEditor());
	}
	
	/**
	 * This class represents the model where all cells are editable and animations are always grouped
	 * by object.
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static final class AnimationsTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 8285912542455513806L;

		/**
		 * Constructor. Creates table with given columns and no rows.
		 * @param columnNames the names of the columns
		 */
		public AnimationsTableModel(Object[] columnNames) {
			super(columnNames, 0);
		}
		
		@Override
		public boolean isCellEditable(int row, int column) {
			return true;
		}
		
		@Override
		@SuppressWarnings("rawtypes")
		public void addRow(Vector rowData) {
			String objectName = (String) rowData.get(1);
			int index = 0;
			boolean objectFound = false;
			for(int i=0;i<this.getRowCount();++i) {
				String name = (String)this.getValueAt(i, 1);
				if(name.equals(objectName)) {
					index = i;
					objectFound = true;
				}
			}
			if(objectFound) {
				this.insertRow(index + 1, rowData);
			} else {
				super.addRow(rowData);
			}
		}
	}
	
	/**
	 * This class renders each group (specified by object) to one of the colors so that they are
	 * easily recognizable. It also renderes selected row with JRadioButton.
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static final class CellRenderer extends DefaultTableCellRenderer implements TableModelListener {
		private static final long serialVersionUID = 3759759133199203533L;
		
		/** Index of the object (specifies the object's group color. */
		private Map<String, Integer> objectIndex = new HashMap<String, Integer>();
		/** The other color for the group (the first one is WHITE. */
		private Color color = new Color(240, 240, 240);
		/** Radio button to display row selection. */
		private JRadioButton jRadioButton = new JRadioButton();
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Object objectName = table.getModel().getValueAt(row, 1);
			Component component;
			if(column == 0) {
				jRadioButton.setSelected((Boolean)value);
				component = jRadioButton;
			} else {
				component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
			
			Integer index = objectIndex.get(objectName);
			if(index != null) {
				if(index.intValue() % 2 == 1) {
					component.setBackground(color);
				} else {
					component.setBackground(Color.WHITE);
				}
			}
			return component;
		}
		
		@Override
		public void tableChanged(TableModelEvent evt) {
			if(evt.getType() == TableModelEvent.INSERT) {
				DefaultTableModel model = (DefaultTableModel)evt.getSource();
				for(int i=evt.getFirstRow();i<=evt.getLastRow();++i) {
					String objectName = (String) model.getValueAt(i, 1);
					if(!objectIndex.containsKey(objectName)) {
						objectIndex.put(objectName, Integer.valueOf(objectIndex.size()));
					}
				}
			}
		}
	}
	
	/**
	 * This editor is used for the first column to allow the edition of row selection.
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static final class RadioButtonCellEditor extends DefaultCellEditor {
		private static final long serialVersionUID = 7697027333456874718L;
		/** Component that allows editing. */
		private JRadioButton jRadioButton = new JRadioButton();
		
		/**
		 * Constructor.
		 */
		public RadioButtonCellEditor() {
			super(new JTextField());
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			jRadioButton.setSelected((Boolean)value);
			return jRadioButton;
		}
		
		@Override
		public Object getCellEditorValue() {
			return Boolean.valueOf(jRadioButton.isSelected());
		}
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		Random random = new Random(System.currentTimeMillis());
		AnimationsTable table = new AnimationsTable();
		int objectsCount = random.nextInt(5) + 1;
		for(int i=1;i<=objectsCount;++i) {
			int animsCount = random.nextInt(7) + 1;
			for(int j=1;j<=animsCount;++j) {
				((DefaultTableModel)table.getModel()).addRow(new Object[] {Boolean.FALSE, "Obiekt" + i, "Animacja" + j, "Start" + j, "Stop" + j});
			}
		}
		((DefaultTableModel)table.getModel()).addRow(new Object[] {Boolean.FALSE, "Obiekt1", "xxx", "xxx", "xxx"});
		JScrollPane jScrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frame.add(jScrollPane, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
