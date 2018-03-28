package jpl.mipl.mdms.FileService.komodo.ui.savannah.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahList;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahListException;
import jpl.mipl.mdms.FileService.komodo.ui.savannah.SavannahModel;

/**
 * 
 * This class is used to handle the transfer of a Transferable to and from
 * SavannahList components. The Transferable is used to represent data that is
 * exchanged via drag-and-drop operations to represent a drag from a component,
 * and a drop to a component. Swing also provides functionality that
 * automatically supports drag and drop that uses the functionality provided by
 * an implementation of this class.
 * 
 * @author Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)
 * @version $Id: SavannahTableTransferHandler.java,v 1.4 2005/07/30 01:11:07 ntt Exp $
 */

public class SavannahTableTransferHandler extends TransferHandler {
   private final String __classname = "SavannahTableTransferHandler";

   DataFlavor _localListFlavor, _serialListFlavor;
   DataFlavor _sourceDataFlavor, _stringFlavor;
   SavannahModel _model;
   SavannahList _savannahList;

   //---------------------------------------------------------------------

   /**
    * Constructor.
    * 
    * @param model Application model for Savannah package.
    * @param list Instance of SavannahList that can partake in DnD.
    */

   public SavannahTableTransferHandler(SavannahModel model, SavannahList list) {
      if (model == null) {
         throw new IllegalArgumentException(__classname + "::constructor::"
               + "Parameter 'model' cannot be null.");
      }
      if (list == null) {
         throw new IllegalArgumentException(__classname + "::constructor::"
               + "Parameter 'list' cannot be null.");
      }
      _model = model;
      _savannahList = list;

      _localListFlavor = new DataFlavor(
            DNDListSourceTransferable.LOCAL_LIST_TYPE,
            DNDListSourceTransferable.LOCAL_HUMAN_READABLE_NAME);
      _serialListFlavor = new DataFlavor(
            DNDListSourceTransferable.SERIAL_CLASS_TYPE,
            DNDListSourceTransferable.SERIAL_HUMAN_READABLE_NAME);
      _sourceDataFlavor = new DataFlavor(DNDSourceIdentifier.class,
            DNDSourceIdentifier.HUMAN_READABLE);
      _stringFlavor = DataFlavor.stringFlavor;
   }

   //---------------------------------------------------------------------

   /**
    * Import data from transferable to component
    * 
    * @param c Instance of component that is importing the data
    * @param t Instance of Transferable containing data
    */

   public boolean importData(JComponent c, Transferable t) {
      DataFlavor[] flavors = t.getTransferDataFlavors();

      List alist = null;
      DNDSourceIdentifier id = null;

      String filename;
      String sourceName = null;
      String targetName = c.getName(); //my name

      boolean canImport = canImport(c, flavors);

      if (!canImport) {
         _model.setStatusMessage("Drag cancelled.");
         return false;
      }

      _model.printDebug(__classname + "::importData() : 2");

      try {

         if (hasLocalListFlavor(flavors)) {
            _model.printDebug(__classname + "::importData() : 2.2");
            alist = (List) t.getTransferData(_localListFlavor);
         }
         if (hasSerialListFlavor(flavors)) {
            _model.printDebug(__classname + "::importData() : 2.4");
            alist = (List) t.getTransferData(_serialListFlavor);
         }
         if (hasDNDSourceIdentifierFlavor(flavors)) {
            _model.printDebug(__classname + "::importData() : 2.6");
            id = (DNDSourceIdentifier) t.getTransferData(_sourceDataFlavor);
            if (id != null) {
               _model.printDebug(__classname + "::importData() : 2.6.2");
               sourceName = id.getName();
            }
         }
         if (hasStringFlavor(flavors)) {
            _model.printDebug(__classname + "::importData() : 2.7");
            String listString = (String) t.getTransferData(_stringFlavor);
            if (listString != null) {
               _model.printDebug(__classname + "::importData() : 2.7.2");
               alist = DNDListSourceTransferable.stringToList(listString);
            }
         } else {
            _model.setStatusMessage("Drag cancelled.");
            return false;
         }
      } catch (UnsupportedFlavorException ufe) {
         _model.printDebug("importData: unsupported data flavor");
         _model.setStatusMessage("Drag cancelled.");
         return false;
      } catch (IOException ioe) {
         _model.printDebug("importData: I/O exception");
         _model.setStatusMessage("Drag cancelled.");
         return false;
      }

      _model.printDebug(__classname + "::importData() : 4" + "\n SRC:"
            + sourceName + "\n TRG:" + targetName);

      //Prevent the user from dropping data back on itself.
      if (targetName.equals(sourceName)) {
         _model.printDebug(__classname + "::importData() : Source == Target");
         _model.setStatusMessage("Drag cancelled.");
         return false;
      }
      if (alist == null || alist.isEmpty()) {
         _model.setStatusMessage("Drag cancelled.");
         return false;
      }

      _model.printDebug(__classname + "::importData() : 6");

      canImport = false;

      try {
         canImport = _savannahList.canImport(alist);
      } catch (SavannahListException slEx) {
         JOptionPane.showMessageDialog((JComponent) slEx.getList(), slEx
               .getMessage()
               + "\n\nDrag aborted.", "Drag Warning",
               JOptionPane.WARNING_MESSAGE);
         return false;
      }

      int numFiles = alist.size();

      if (numFiles == 0) {
         _model.setStatusMessage("Drag cancelled.");
         return false;
      }

      String[] filenames = new String[numFiles];

      for (int i = 0; i < numFiles; ++i) {
         filenames[i] = (String) alist.get(i);
      }

      _savannahList.importEntry(filenames);

      _model.printDebug(__classname + "::importData() : 8");
      return true;
   }

   //---------------------------------------------------------------------

   protected void exportDone(JComponent c, Transferable data, int action) {
      super.exportDone(c, data, action);
   }

   //---------------------------------------------------------------------

   private boolean hasLocalListFlavor(DataFlavor[] flavors) {
      if (_localListFlavor == null) {
         return false;
      }

      for (int i = 0; i < flavors.length; i++) {
         if (flavors[i].equals(_localListFlavor)) {
            return true;
         }
      }
      return false;
   }

   //---------------------------------------------------------------------

   private boolean hasSerialListFlavor(DataFlavor[] flavors) {
      if (_serialListFlavor == null) {
         return false;
      }

      for (int i = 0; i < flavors.length; i++) {
         if (flavors[i].equals(_serialListFlavor)) {
            return true;
         }
      }
      return false;
   }

   //---------------------------------------------------------------------

   private boolean hasDNDSourceIdentifierFlavor(DataFlavor[] flavors) {
      if (_sourceDataFlavor == null) {
         return false;
      }

      for (int i = 0; i < flavors.length; i++) {
         if (flavors[i].equals(_sourceDataFlavor)) {
            return true;
         }
      }
      return false;
   }

   //---------------------------------------------------------------------

   private boolean hasStringFlavor(DataFlavor[] flavors) {
      if (_stringFlavor == null) {
         return false;
      }

      for (int i = 0; i < flavors.length; i++) {
         if (flavors[i].equals(_stringFlavor)) {
            return true;
         }
      }
      return false;
   }

   //---------------------------------------------------------------------

   public boolean canImport(JComponent c, DataFlavor[] flavors) {
      if (hasLocalListFlavor(flavors)) {
         return true;
      }
      if (hasSerialListFlavor(flavors)) {
         return true;
      }
      if (hasDNDSourceIdentifierFlavor(flavors)) {
         return true;
      }
      if (hasStringFlavor(flavors)) {
         return true;
      }
      return false;
   }

   //---------------------------------------------------------------------

   protected Transferable createTransferable(JComponent c) {
      _model.printDebug(__classname + "::createTransferable() : "
            + "Hello Jerry: " + c);

      //get selected values (filepaths or filenames)
      Object[] selected = this._savannahList.getSelectedValues();

      //if null or empty, cancel
      if (selected == null || selected.length == 0) {
         _model.setStatusMessage("Drag cancelled.");
         return null;
      }

      boolean canExport;
      try {
         canExport = _savannahList.canExport(selected);
      } catch (SavannahListException slEx) {

         String message = slEx.getMessage();

         //display message only if the error does not contain the string
         //below. This string occurs for moving folders.
         if (message.indexOf("are not transferrable.") == -1) {
            JOptionPane.showMessageDialog((JComponent) slEx.getList(), slEx
                  .getMessage()
                  + "\n\nAborting drag.", "Drag Warning",
                  JOptionPane.WARNING_MESSAGE);
         }
         canExport = false;
      }

      if (!canExport) {
         _model.setStatusMessage("Drag cancelled.");
         return null;
      }

      List files = new Vector();
      String entry;
      for (int i = 0; i < selected.length; ++i) {
         if (selected[i] instanceof File)
            entry = ((File) selected[i]).getAbsolutePath();
         else
            entry = selected[i].toString();
         files.add(entry);
      }

      if (!files.isEmpty()) {
         DNDSourceIdentifier id;
         id = new DNDSourceIdentifier(c.getName());

         DNDListSourceTransferable transfer = new DNDListSourceTransferable(
               files, id);
         return transfer;
      }

      _model.setStatusMessage("Drag cancelled.");
      return null;
   }

   //---------------------------------------------------------------------

   public int getSourceActions(JComponent c) {
      return COPY;
   }

   //---------------------------------------------------------------------
}

