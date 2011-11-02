package org.multibit.viewsystem.swing.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.multibit.Localiser;
import org.multibit.controller.MultiBitController;
import org.multibit.model.AddressBookData;
import org.multibit.model.Data;
import org.multibit.model.DataProvider;
import org.multibit.model.Item;
import org.multibit.model.MultiBitModel;
import org.multibit.model.WalletInfo;
import org.multibit.qrcode.BitcoinURI;
import org.multibit.qrcode.QRCodeEncoderDecoder;
import org.multibit.qrcode.SwatchGenerator;
import org.multibit.utils.WhitespaceTrimmer;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.swing.MultiBitFrame;
import org.multibit.viewsystem.swing.action.CopyQRCodeImageAction;
import org.multibit.viewsystem.swing.action.CopyQRCodeTextAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Address;

/**
 * Abstract parent class for SendBitcoinPanel and ReceiveBitcoinPanel
 * 
 * @author jim
 * 
 */
public abstract class AbstractTradePanel extends JPanel implements View, DataProvider {

    private static final long serialVersionUID = 7227169670412230264L;

    private static final Logger log = LoggerFactory.getLogger(AbstractTradePanel.class);

    protected MultiBitFrame mainFrame;

    protected MultiBitController controller;

    protected JTextArea labelTextArea;

    protected JTextField amountTextField;

    protected JPanel formPanel;

    protected AddressBookTableModel addressesTableModel;

    protected JTable addressesTable;

    protected JTextField addressTextField;
    protected JTextArea addressTextArea;

    protected int selectedAddressRow;

    protected SelectionListener addressesListener;

    protected JButton createNewButton;

    protected JLabel titleLabel;

    protected JPanel qrCodePanel;
    protected JLabel qrCodeLabel;

    protected static final int QRCODE_WIDTH = 140;
    protected static final int QRCODE_HEIGHT = 140;
    protected static final int MINIMUM_QRCODE_PANEL_HORIZONTAL_SPACING = 30;
    protected static final int MINIMUM_QRCODE_PANEL_VERTICAL_SPACING = 80;

    protected JButton copyQRCodeTextButton;

    private final AbstractTradePanel thisAbstractTradePanel;

    private SwatchGenerator swatchGenerator;

    /**
     * map that maps one of the key constants in this class to the actual key to
     * use for localisation
     * 
     * this map is filled up in the constructors of the concrete impls
     */
    protected Map<String, String> localisationKeyConstantToKeyMap;

    protected String ADDRESSES_TITLE = "addressesTitle";
    protected String CREATE_NEW_TOOLTIP = "createNewTooltip";

    public AbstractTradePanel(MultiBitFrame mainFrame, MultiBitController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        this.thisAbstractTradePanel = this;

        localisationKeyConstantToKeyMap = new HashMap<String, String>();
        populateLocalisationMap();

        initUI();
        loadForm();

        labelTextArea.requestFocusInWindow();
    }

    /**
     * is it the receive bitcion panel (return true) or the send bitcoin panel
     * (return false)
     */
    protected abstract boolean isReceiveBitcoin();

    protected abstract String getAddressConstant();

    protected abstract String getLabelConstant();

    protected abstract String getAmountConstant();

    protected abstract String getUriImageConstant();

    protected abstract Action getCreateNewAddressAction();

    /**
     * method for concrete impls to populate the localisation map
     */
    protected abstract void populateLocalisationMap();

    /**
     * get a localisation string - the key varies according to the concrete impl
     */
    protected String getLocalisationString(String keyConstant, Object[] data) {
        String stringToReturn = "";
        // get the localisation key
        if (localisationKeyConstantToKeyMap != null && keyConstant != null) {
            String key = localisationKeyConstantToKeyMap.get(keyConstant);
            stringToReturn = controller.getLocaliser().getString(key, data);
        }
        return stringToReturn;
    }

    protected void initUI() {
        setMinimumSize(new Dimension(550, 220));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        setLayout(new GridBagLayout());
        setBackground(MultiBitFrame.BACKGROUND_COLOR);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.weighty = 0.4;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(createFormPanel(), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.weighty = 0.4;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(createQRCodePanel(), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.weighty = 1.2;
        constraints.anchor = GridBagConstraints.LINE_START;
        add(createAddressesPanel(), constraints);
    }

    protected abstract JPanel createFormPanel();

    public abstract void loadForm();

    protected JPanel createAddressesHeaderPanel() {
        JPanel addressesHeaderPanel = new AddressesPanel();

        addressesHeaderPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        JLabel filler1 = new JLabel("");
        filler1.setOpaque(false);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.01;
        constraints.weighty = 0.01;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(filler1, constraints);

        createNewButton = new JButton(getCreateNewAddressAction());
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.3;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(createNewButton, constraints);

        titleLabel = new JLabel();
        titleLabel.setHorizontalTextPosition(JLabel.CENTER);
        titleLabel.setText(getLocalisationString(ADDRESSES_TITLE, null));
        Font font = new Font(MultiBitFrame.MULTIBIT_FONT_NAME, MultiBitFrame.MULTIBIT_FONT_STYLE,
                MultiBitFrame.MULTIBIT_LARGE_FONT_SIZE + 2);
        titleLabel.setFont(font);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        addressesHeaderPanel.add(titleLabel, constraints);

        JPanel filler2 = new JPanel();
        filler2.setOpaque(false);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.6;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressesHeaderPanel.add(filler2, constraints);

        return addressesHeaderPanel;
    }

    protected JPanel createAddressesPanel() {
        JPanel addressPanel = new JPanel();
        addressPanel.setOpaque(true);
        addressPanel.setBackground(MultiBitFrame.BACKGROUND_COLOR);

        addressPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        // get the stored previously selected receive address

        addressPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        addressesTableModel = new AddressBookTableModel(controller, isReceiveBitcoin());
        addressesTable = new JTable(addressesTableModel);
        addressesTable.setOpaque(true);
        addressesTable.setShowGrid(false);
        addressesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        addressesTable.setRowSelectionAllowed(true);
        addressesTable.setColumnSelectionAllowed(false);
        // TODO make sure table cannot be edited by double click

        TableColumn tableColumn = addressesTable.getColumnModel().getColumn(0); // label
        tableColumn.setPreferredWidth(40);
        // label left justified
        tableColumn.setCellRenderer(new LeftJustifiedRenderer());

        tableColumn = addressesTable.getColumnModel().getColumn(1); // address
        tableColumn.setPreferredWidth(120);
        // addresses left justified
        tableColumn.setCellRenderer(new LeftJustifiedRenderer());

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.05;
        constraints.anchor = GridBagConstraints.LINE_START;
        addressPanel.add(createAddressesHeaderPanel(), constraints);

        JScrollPane scrollPane = new JScrollPane(addressesTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(MultiBitFrame.BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MultiBitFrame.DARK_BACKGROUND_COLOR.darker()));

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        addressPanel.add(scrollPane, constraints);

        // add on a selection listener
        addressesListener = new SelectionListener();
        addressesTable.getSelectionModel().addListSelectionListener(addressesListener);

        return addressPanel;
    }

    class SelectionListener implements ListSelectionListener {
        SelectionListener() {
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getSource() instanceof DefaultListSelectionModel && !e.getValueIsAdjusting()) {
                // Column selection changed
                int firstIndex = e.getFirstIndex();
                int lastIndex = e.getLastIndex();

                if (selectedAddressRow == firstIndex) {
                    selectedAddressRow = lastIndex;
                } else {
                    if (selectedAddressRow == lastIndex) {
                        selectedAddressRow = firstIndex;
                    }
                }
                AddressBookData rowData = addressesTableModel.getAddressBookDataByRow(selectedAddressRow,
                        thisAbstractTradePanel.isReceiveBitcoin());
                if (rowData != null) {
                    controller.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAddressConstant(),
                            rowData.getAddress());
                    controller.getModel().setActiveWalletPreference(thisAbstractTradePanel.getLabelConstant(),
                            rowData.getLabel());
                    if (addressTextArea != null) {
                        addressTextArea.setText(rowData.getAddress());
                    }
                    if (addressTextField != null) {
                        addressTextField.setText(rowData.getAddress());
                    }
                    labelTextArea.setText(rowData.getLabel());

                    displaySwatch(rowData.getAddress(), amountTextField.getText(), labelTextArea.getText());
                }
            }
        }
    }

    class LeftJustifiedRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1549545L;

        JLabel label = new JLabel();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setOpaque(true);

            label.setText((String) value);

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                Color backgroundColor = (row % 2 == 0 ? Color.WHITE : MultiBitFrame.BACKGROUND_COLOR);
                label.setBackground(backgroundColor);
                label.setForeground(table.getForeground());
            }
            return label;
        }
    }

    protected JPanel createQRCodePanel() {
        qrCodePanel = new JPanel();
        qrCodePanel.setBackground(MultiBitFrame.VERY_LIGHT_BACKGROUND_COLOR);
        qrCodePanel.setOpaque(true);
        qrCodePanel.setMinimumSize(new Dimension(280, 200));
        qrCodePanel.setLayout(new GridBagLayout());
        qrCodeLabel = new JLabel("", null, JLabel.CENTER);
        qrCodeLabel.setMinimumSize(new Dimension(QRCODE_WIDTH, QRCODE_HEIGHT));

        qrCodeLabel.setVerticalTextPosition(JLabel.TOP);
        qrCodeLabel.setHorizontalTextPosition(JLabel.CENTER);
        qrCodeLabel.setBackground(MultiBitFrame.VERY_LIGHT_BACKGROUND_COLOR);
        qrCodeLabel.setOpaque(true);

        if (!isReceiveBitcoin()) {
            qrCodeLabel.setText(controller.getLocaliser().getString("sendBitcoinPanel.dragBitcoinLabel.text"));
            qrCodeLabel.setToolTipText(controller.getLocaliser().getString("sendBitcoinPanel.dragBitcoinLabel.tooltip"));
        }

        // copy/ drag image support
        if (isReceiveBitcoin()) {
            qrCodeLabel.setTransferHandler(new ImageSelection1(false));
        } else {
            qrCodeLabel.setTransferHandler(new ImageSelection());
        }

        // drag support
        MouseListener listener = new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JComponent comp = (JComponent) me.getSource();
                TransferHandler handler = comp.getTransferHandler();
                handler.exportAsDrag(comp, me, TransferHandler.COPY);
            }
        };
        qrCodeLabel.addMouseListener(listener);

        GridBagConstraints constraints = new GridBagConstraints();

        JPanel filler1 = new JPanel();
        filler1.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.02;
        constraints.weighty = 0.02;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        qrCodePanel.add(filler1, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;

        JScrollPane scrollPane = new JScrollPane(qrCodeLabel);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(MultiBitFrame.VERY_LIGHT_BACKGROUND_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        scrollPane.setMinimumSize(new Dimension(200, 160));

        qrCodePanel.add(scrollPane, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        JPanel qrCodeButtonPanel = createQRCodeButtonPanel();
        qrCodeButtonPanel.setOpaque(false);
        qrCodePanel.add(qrCodeButtonPanel, constraints);

        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.weightx = 0.05;
        constraints.weighty = 0.02;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        qrCodePanel.add(filler3, constraints);

        return qrCodePanel;
    }

    protected JPanel createQRCodeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        buttonPanel.setLayout(flowLayout);

        CopyQRCodeTextAction copyQRCodeTextAction = new CopyQRCodeTextAction(controller, this);
        copyQRCodeTextButton = new JButton(copyQRCodeTextAction);
        buttonPanel.add(copyQRCodeTextButton);

        CopyQRCodeImageAction copyQRCodeImageAction = new CopyQRCodeImageAction(controller, this);
        JButton copyQRCodeImageButton = new JButton(copyQRCodeImageAction);
        buttonPanel.add(copyQRCodeImageButton);

        return buttonPanel;
    }

    @Override
    public void displayView() {
        loadForm();
        selectRows();
        updateView();
    }

    @Override
    public void updateView() {
        // disable any new changes if another process has changed the wallet
        if (controller.getModel().getActivePerWalletModelData() != null
                && controller.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
            // files have been changed by another process - disallow edits
            mainFrame.setUpdatesStoppedTooltip(labelTextArea);
            labelTextArea.setEditable(false);
            labelTextArea.setEnabled(false);
            mainFrame.setUpdatesStoppedTooltip(amountTextField);
            amountTextField.setEditable(false);
            amountTextField.setEnabled(false);

            if (createNewButton != null) {
                createNewButton.setEnabled(false);
                mainFrame.setUpdatesStoppedTooltip(createNewButton);
            }
        } else {
            labelTextArea.setToolTipText(null);
            labelTextArea.setEditable(true);
            labelTextArea.setEnabled(true);
            amountTextField.setToolTipText(null);
            amountTextField.setEditable(true);
            amountTextField.setEnabled(true);
            if (createNewButton != null) {
                createNewButton.setEnabled(true);
                createNewButton.setToolTipText(getLocalisationString(CREATE_NEW_TOOLTIP, null));
            }
        }
    }

    @Override
    public void navigateAwayFromView(int nextViewId, int relationshipOfNewViewToPrevious) {
        // save any changes
        if (controller.getModel().getActivePerWalletModelData() != null
                && controller.getModel().getActivePerWalletModelData().isDirty()) {
            controller.getFileHandler().savePerWalletModelData(controller.getModel().getActivePerWalletModelData(), false);
        }
    }

    @Override
    public void displayMessage(String messageKey, Object[] messageData, String titleKey) {
    }

    protected class QRCodeKeyListener implements KeyListener {
        /** Handle the key typed event from the text field. */
        public void keyTyped(KeyEvent e) {
        }

        /** Handle the key-pressed event from the text field. */
        public void keyPressed(KeyEvent e) {
            // do nothing
        }

        /** Handle the key-released event from the text field. */
        public void keyReleased(KeyEvent e) {
            String address = null;
            if (addressTextArea != null) {
                address = addressTextArea.getText();
            }
            if (addressTextField != null) {
                address = addressTextField.getText();
            }
            String amount = amountTextField.getText();
            String label = labelTextArea.getText();
            AddressBookData addressBookData = new AddressBookData(label, address);

            WalletInfo walletInfo = controller.getModel().getActiveWalletWalletInfo();
            if (walletInfo == null) {
                walletInfo = new WalletInfo(controller.getModel().getActiveWalletFilename());
                controller.getModel().setActiveWalletInfo(walletInfo);
            }
            address = WhitespaceTrimmer.trim(address);
            addressesTableModel.setAddressBookDataByRow(addressBookData, selectedAddressRow, isReceiveBitcoin());
            controller.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAddressConstant(), address);
            controller.getModel().setActiveWalletPreference(thisAbstractTradePanel.getLabelConstant(), label);
            controller.getModel().setActiveWalletPreference(thisAbstractTradePanel.getAmountConstant(), amount);
            controller.getModel().getActivePerWalletModelData().setDirty(true);

            displaySwatch(address, amount, label);
        }
    }

    /**
     * display the address, amount and label as a swatch
     */
    protected void displaySwatch(String address, String amount, String label) {
        if (swatchGenerator == null) {
            swatchGenerator = new SwatchGenerator();
        }
        try {
            BufferedImage image = swatchGenerator.generateSwatch(address, amount, label);
            ImageIcon icon;
            if (image != null) {
                icon = new ImageIcon(image);
            } else {
                icon = new ImageIcon();
            }
            qrCodeLabel.setIcon(icon);
        } catch (IllegalArgumentException iae) {
            log.error(iae.getMessage(), iae);
        }
    }

    /**
     * select the rows that correspond to the current data
     */
    public void selectRows() {
        // stop listener firing
        addressesTable.getSelectionModel().removeListSelectionListener(addressesListener);

        String address = controller.getModel().getActiveWalletPreference(getAddressConstant());
        displaySwatch(address, amountTextField.getText(), labelTextArea.getText());

        // see if the current address is on the table and select it
        int rowToSelect = addressesTableModel.findRowByAddress(address, isReceiveBitcoin());
        if (rowToSelect >= 0) {
            addressesTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
            selectedAddressRow = rowToSelect;
        }

        // scroll to visible
        addressesTable.scrollRectToVisible(addressesTable.getCellRect(rowToSelect, 0, false));
        // put the listeners back
        addressesTable.getSelectionModel().addListSelectionListener(addressesListener);
    }

    public Data getData() {
        Data data = new Data();
        
        Item isReceiveBitcoinItem = new Item(MultiBitModel.IS_RECEIVE_BITCOIN);
        isReceiveBitcoinItem.setNewValue(Boolean.toString(isReceiveBitcoin()));
        data.addItem(MultiBitModel.IS_RECEIVE_BITCOIN, isReceiveBitcoinItem);
        
        Item addressItem = new Item(getAddressConstant());
        if (addressTextArea != null) {
            addressItem.setNewValue(addressTextArea.getText());
        }
        if (addressTextField != null) {
            addressItem.setNewValue(addressTextField.getText());
        }
        data.addItem(getAddressConstant(), addressItem);

        Item labelItem = new Item(getLabelConstant());
        labelItem.setNewValue(labelTextArea.getText());
        data.addItem(getLabelConstant(), labelItem);

        Item amountItem = new Item(getAmountConstant());
        amountItem.setNewValue(amountTextField.getText());
        data.addItem(getAmountConstant(), amountItem);

        Item uriImageItem = new Item(getUriImageConstant());
        uriImageItem.setNewValue(qrCodeLabel);
        data.addItem(getUriImageConstant(), uriImageItem);

        return data;
    }

    public JTextArea getLabelTextArea() {
        return labelTextArea;
    }

    public JPanel getFormPanel() {
        return formPanel;
    }
    
    class ImageSelection extends TransferHandler implements Transferable {
        private static final long serialVersionUID = 756395092284264645L;

        private DataFlavor urlFlavor;
        private DataFlavor uriListAsStringFlavor;
        private DataFlavor uriListAsReaderFlavor;

        private DataFlavor flavors[];
        
        private Image image;

        public ImageSelection() {
            try {
                urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
                uriListAsStringFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
                uriListAsReaderFlavor = new DataFlavor("text/uri-list;class=java.io.Reader");
                flavors = new DataFlavor[] { DataFlavor.imageFlavor, urlFlavor, uriListAsStringFlavor,
                        uriListAsReaderFlavor };
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }

        public boolean canImport(JComponent comp, DataFlavor flavor[]) {
            if (!(comp instanceof JLabel) && !(comp instanceof AbstractButton)) {
                return false;
            }

            for (int i = 0, n = flavor.length; i < n; i++) {
                for (int j = 0, m = flavors.length; j < m; j++) {
                    if (flavor[i].equals(flavors[j])) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Transferable createTransferable(JComponent comp) {
            // Clear
            image = null;

            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                Icon icon = label.getIcon();
                if (icon instanceof ImageIcon) {
                    image = ((ImageIcon) icon).getImage();
                    return this;
                }
            }
            return null;
        }

        private boolean processDecodedString(String decodedString, JLabel label, ImageIcon icon) {
            // decode the string to an AddressBookData
            BitcoinURI bitcoinURI = new BitcoinURI(controller, decodedString);

            if (bitcoinURI.isParsedOk()) {
                log.debug("SendBitcoinPanel - ping 1");
                Address address = bitcoinURI.getAddress();
                log.debug("SendBitcoinPanel - ping 2");
                String addressString = address.toString();
                log.debug("SendBitcoinPanel - ping 3");
                String amountString = amountTextField.getText();
                if (bitcoinURI.getAmount() != null) {
                    amountString = Localiser.bitcoinValueToString4(bitcoinURI.getAmount(), false, false);
                }
                log.debug("SendBitcoinPanel - ping 4");
                String decodedLabel = bitcoinURI.getLabel();

                log.debug("SendBitcoinPanel#imageSelection#importData = addressString = " + addressString + ", amountString = "
                        + amountString + ", label = " + decodedLabel);
                log.debug("SendBitcoinPanel - ping 5");

                AddressBookData addressBookData = new AddressBookData(decodedLabel, addressString);
                log.debug("SendBitcoinPanel - ping 6");
                // see if the address is already in the address book
                // see if the current address is on the table and
                // select it
                int rowToSelect = addressesTableModel.findRowByAddress(addressBookData.getAddress(), false);
                if (rowToSelect >= 0) {
                    addressesTableModel.setAddressBookDataByRow(addressBookData, rowToSelect, false);
                    addressesTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
                    selectedAddressRow = rowToSelect;
                } else {
                    // add a new row to the table
                    controller.getModel().getActiveWalletWalletInfo().addSendingAddress(addressBookData);
                    controller.getModel().getActivePerWalletModelData().setDirty(true);

                    // select new row
                    rowToSelect = addressesTableModel.findRowByAddress(addressBookData.getAddress(), false);
                    if (rowToSelect >= 0) {
                        addressesTable.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
                        selectedAddressRow = rowToSelect;
                    }
                }
                // scroll to visible
                addressesTable.scrollRectToVisible(addressesTable.getCellRect(rowToSelect, 0, false));
                addressesTable.invalidate();
                addressesTable.validate();
                addressesTable.repaint();
                mainFrame.invalidate();
                mainFrame.validate();
                mainFrame.repaint();

                log.debug("SendBitcoinPanel - ping 7");
                controller.getModel().setActiveWalletPreference(MultiBitModel.SEND_ADDRESS, addressString);
                log.debug("SendBitcoinPanel - ping 8");
                controller.getModel().setActiveWalletPreference(MultiBitModel.SEND_LABEL, decodedLabel);
                log.debug("SendBitcoinPanel - ping 9");

                controller.getModel().setActiveWalletPreference(MultiBitModel.SEND_AMOUNT, amountString);
                log.debug("SendBitcoinPanel - ping 10");
                addressTextField.setText(addressString);
                log.debug("SendBitcoinPanel - ping 11");
                amountTextField.setText(amountString);
                log.debug("SendBitcoinPanel - ping 12");
                labelTextArea.setText(decodedLabel);
                log.debug("SendBitcoinPanel - ping 13");
                mainFrame.updateStatusLabel("");
                label.setIcon(icon);
                label.setToolTipText(decodedString);
                return true;
            } else {
                mainFrame.updateStatusLabel(controller.getLocaliser().getString("sendBitcoinPanel.couldNotUnderstandQRcode",
                        new Object[] { decodedString }));
                return false;
            }
        }

        public boolean importData(JComponent comp, Transferable transferable) {
            if (comp instanceof JLabel) {
                log.debug("importData - 1");

                JLabel label = (JLabel) comp;
                image = getDropData(transferable, label);
                log.debug("importData - 2 - image = " + image);

                if (image != null) {
                    BufferedImage bufferedImage;
                    log.debug("importData - 2.1");
                    if (image.getWidth(qrCodeLabel) + MINIMUM_QRCODE_PANEL_HORIZONTAL_SPACING > qrCodePanel.getWidth()
                            || image.getHeight(qrCodeLabel) + MINIMUM_QRCODE_PANEL_VERTICAL_SPACING > qrCodePanel.getHeight()) {
                        // scale image
                        double qrCodeWidth = (double) qrCodePanel.getWidth();
                        double qrCodeHeight = (double) qrCodePanel.getHeight();
                        double xScale = qrCodeWidth
                                / (double) (image.getWidth(qrCodeLabel) + MINIMUM_QRCODE_PANEL_HORIZONTAL_SPACING);
                        double yScale = qrCodeHeight
                                / (double) (image.getHeight(qrCodeLabel) + MINIMUM_QRCODE_PANEL_VERTICAL_SPACING);
                        double scaleFactor = Math.min(xScale, yScale);
                        bufferedImage = toBufferedImage(image, (int) (image.getWidth(qrCodeLabel) * scaleFactor),
                                (int) (image.getHeight(qrCodeLabel) * scaleFactor));
                    } else {
                        // no resize
                        bufferedImage = toBufferedImage(image, -1, -1);
                    }
                    log.debug("importData - 2.2");
                    ImageIcon icon = new ImageIcon(bufferedImage);

                    // decode the QRCode to a String
                    QRCodeEncoderDecoder qrCodeEncoderDecoder = new QRCodeEncoderDecoder(image.getWidth(qrCodeLabel),
                            image.getHeight(qrCodeLabel));
                    log.debug("importData - 2.3");

                    String decodedString = qrCodeEncoderDecoder.decode(toBufferedImage(image, -1, -1));
                    log.debug("importData - 3 - decodedResult = " + decodedString);
                    log.info("SendBitcoinPanel#imageSelection#importData = decodedString = {}", decodedString);
                    return processDecodedString(decodedString, label, icon);
                }
            }
            return false;
        }

        @SuppressWarnings("rawtypes")
        private Image getDropData(Transferable transferable, JComponent label) {
            try {
                // try to get an image
                if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    log.debug("image flavor is supported");
                    Image img = (Image) transferable.getTransferData(DataFlavor.imageFlavor);
                    if (img != null && img.getWidth(null) != -1) {
                        return img;
                    }
                }
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    log.debug("javaFileList is supported");
                    java.util.List list = (java.util.List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    for (Object aList : list) {
                        File f = (File) aList;
                        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                        if (icon.getImage() != null) {
                            return icon.getImage();
                        }
                    }
                }

                if (transferable.isDataFlavorSupported(uriListAsStringFlavor)) {
                    log.debug("uriListAsStringFlavor is supported");
                    String uris = (String) transferable.getTransferData(uriListAsStringFlavor);

                    // url-lists are defined by rfc 2483 as crlf-delimited
                    // TODO iterate over list for all of them
                    StringTokenizer izer = new StringTokenizer(uris, "\r\n");
                    if (izer.hasMoreTokens()) {
                        String uri = izer.nextToken();
                        log.debug("uri = " + uri);
                        java.awt.Image image = getURLImage(new URL(uri));

                        if (image != null) {
                            return image;
                        }

                        ImageIcon uriIcon = new ImageIcon(uri);
                        if (uriIcon.getImage() != null) {
                            return uriIcon.getImage();
                        }
                    }
                }

                if (transferable.isDataFlavorSupported(uriListAsReaderFlavor)) {
                    log.debug("uriListAsReaderFlavor is supported");

                    BufferedReader read = new BufferedReader(uriListAsReaderFlavor.getReaderForText(transferable));
                    // Remove 'file://' from file name
                    String fileName = read.readLine().substring(7).replace("%20", " ");
                    // Remove 'localhost' from OS X file names
                    if (fileName.substring(0, 9).equals("localhost")) {
                        fileName = fileName.substring(9);
                    }
                    read.close();

                    java.awt.Image image = getFileImage(new File(fileName));

                    if (image != null) {
                        return image;
                    }
                }

                if (transferable.isDataFlavorSupported(urlFlavor)) {
                    log.debug("urlFlavor is supported");
                    URL url = (URL) transferable.getTransferData(urlFlavor);
                    log.debug("url = " + url);
                    java.awt.Image image = getURLImage(url);

                    if (image != null) {
                        return image;
                    }

                    ImageIcon urlIcon = new ImageIcon(url);
                    if (urlIcon.getImage() != null) {
                        return urlIcon.getImage();
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (UnsupportedFlavorException e) {

                e.printStackTrace();
            }
            return null;
        }

        private Image getURLImage(URL url) {
            Image imageToReturn = null;

            try {
                imageToReturn = ImageIO.read(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return imageToReturn;
        }

        private Image getFileImage(File file) {
            Image imageToReturn = null;

            try {
                imageToReturn = ImageIO.read(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return imageToReturn;
        }

        // Transferable
        public Object getTransferData(DataFlavor flavor) {
            if (isDataFlavorSupported(flavor)) {
                if (DataFlavor.imageFlavor.equals(flavor)) {
                    return image;
                } else {
                    if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                        java.util.List<File> list = new java.util.LinkedList<File>();

                        // write the image to the output stream
                        File swatchFile = new File("swatch.png");
                        try {
                            ImageIO.write(toBufferedImage(image, -1, -1), "png", new File("swatch.png"));
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        list.add(swatchFile);
                        return list;

                    }
                }
            }
            return null;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (int j = 0, m = flavors.length; j < m; j++) {
                if (flavor.equals(flavors[j])) {
                    return true;
                }
            }
            return false;
        }

        public BufferedImage toBufferedImage(Image image, int width, int height) {
            log.debug("SendBitCoinPanel#toBufferedImage - 1");
            if (image == null) {
                return null;
            }
            if (width == -1) {
                width = image.getWidth(null);
            }
            if (height == -1) {
                height = image.getHeight(null);
            }
            // draw original image to thumbnail image object and
            // scale it to the new size on-the-fly
            log.debug("SendBitCoinPanel#toBufferedImage - 2.2, image = " + image + ",width = " + width + ", height = " + height);

            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            log.debug("SendBitCoinPanel#toBufferedImage - 2.3, bufferedImage = " + bufferedImage);

            Graphics2D g2 = bufferedImage.createGraphics();

            log.debug("SendBitCoinPanel#toBufferedImage - 3");
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, 0, 0, width, height, null);
            log.debug("SendBitCoinPanel#toBufferedImage - 4");
            g2.dispose();
            return bufferedImage;
        }

        // This method returns a buffered image with the contents of an image
        public BufferedImage toBufferedImage2(Image image, int width, int height) {
            if (width == -1) {
                width = image.getWidth(null);
            }
            if (height == -1) {
                height = image.getHeight(null);
            }

            // This code ensures that all the pixels in the image are loaded
            image = new ImageIcon(image).getImage();
            log.debug("SendBitCoinPanel#toBufferedImage - 2");

            // Create a buffered image with a format that's compatible with the
            // screen
            BufferedImage bimage = null;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            log.debug("SendBitCoinPanel#toBufferedImage - 2.1");
            try {
                // Determine the type of transparency of the new buffered image
                int transparency = Transparency.OPAQUE;

                // Create the buffered image
                GraphicsDevice gs = ge.getDefaultScreenDevice();
                log.debug("SendBitCoinPanel#toBufferedImage - 2.2");

                GraphicsConfiguration gc = gs.getDefaultConfiguration();
                log.debug("SendBitCoinPanel#toBufferedImage - 2.3, image = " + image + ",width = " + width + ", height = "
                        + height);

                bimage = gc.createCompatibleImage(width, height, transparency);
                log.debug("SendBitCoinPanel#toBufferedImage - 2.4");

            } catch (HeadlessException e) {
                // The system does not have a screen
            }
            log.debug("SendBitCoinPanel#toBufferedImage - 3 - bimage = " + bimage);

            if (bimage == null) {
                // Create a buffered image using the default color model
                int type = BufferedImage.TYPE_INT_RGB;
                bimage = new BufferedImage(width, height, type);
            }

            // Copy image to buffered image
            Graphics2D g = bimage.createGraphics();

            // Paint the image onto the buffered image
            g.drawImage(image, 0, 0, width, height, null);

            g.dispose();

            log.debug("SendBitCoinPanel#toBufferedImage - 4 - bimage = " + bimage);

            return bimage;
        }
    }

}