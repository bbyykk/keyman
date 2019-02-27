namespace com.keyman.dom {
  class SelectionCaret {
    node: Node;
    offset: number;

    constructor(node, offset) {
      this.node = node;
      this.offset = offset;
    }
  }

  class SelectionRange {
    start: SelectionCaret;
    end: SelectionCaret;

    constructor(start, end) {
      this.start = start;
      this.end = end;
    }
  }

  export class ContentEditable extends text.OutputTarget {
    root: HTMLElement;

    constructor(ele: HTMLElement) {
      if(ele.isContentEditable) {
        super();
        this.root = ele;
      } else {
        throw "Specified element is not already content-editable!";
      }
    }

    getElement(): HTMLElement {
      return this.root;
    }

    hasSelection(): boolean {
      let Lsel = this.root.ownerDocument.getSelection();
      
      // We can't completely rely on this.root.contains because of a weird IE 11 bug.
      // Apparently, the text node contains the HTMLElement?
      var ie11ParentChild = function(parent, child) {
        // It's explicitly a text node bug.
        if(child.nodeType != 3) {
          return null;
        }
        let code = child.compareDocumentPosition(parent);

        return (code & 8) != 0; // Yep.  Text node contains its root.
      }

      if(this.root != Lsel.anchorNode && !this.root.contains(Lsel.anchorNode) && !ie11ParentChild(this.root, Lsel.anchorNode)) {
        return false;
      }

      if(this.root != Lsel.focusNode && !this.root.contains(Lsel.focusNode) && !ie11ParentChild(this.root, Lsel.anchorNode)) {
        return false;
      }

      return true;
    }

    clearSelection(): void {
      if(this.hasSelection()) {
        let Lsel = this.root.ownerDocument.getSelection();

        if(!Lsel.isCollapsed) {
          Lsel.deleteFromDocument();  // I2134, I2192
        }
      } else {
        console.warn("Attempted to clear an unowned Selection!");
      }
    }

    invalidateSelection(): void { /* No cache maintenance needed here, partly because
                                   * it's impossible to cache a Selection; it mutates.
                                   */  }

    getCarets(): SelectionRange {
      let Lsel = this.root.ownerDocument.getSelection();
      let code = Lsel.anchorNode.compareDocumentPosition(Lsel.focusNode);

      if(Lsel.isCollapsed) {
        let caret = new SelectionCaret(Lsel.anchorNode, Lsel.anchorOffset);
        return new SelectionRange(caret, caret); 
      } else {
        let anchor = new SelectionCaret(Lsel.anchorNode, Lsel.anchorOffset);
        let focus = new SelectionCaret(Lsel.focusNode, Lsel.focusOffset);

        if(anchor.node == focus.node) {
          code = (focus.offset - anchor.offset > 0) ? 2 : 4;
        }

        if(code & 2) {
          return new SelectionRange(anchor, focus);
        } else { // Default
          // can test against code & 4 to ensure Focus is before anchor, though.
          return new SelectionRange(focus, anchor);
        }
      }
    }

    getDeadkeyCaret(): number {
      return this.getTextBeforeCaret().kmwLength();
    }

    getTextBeforeCaret(): string {
      if(!this.hasSelection()) {
        return;
      }

      let caret = this.getCarets().start;

      if(caret.node.nodeType != 3) {
        return ''; // Must be a text node to provide a context.
      }

      return caret.node.textContent.substr(0, caret.offset);
    }

    getTextAfterCaret(): string {
      if(!this.hasSelection()) {
        return;
      }

      let caret = this.getCarets().end;

      if(caret.node.nodeType != 3) {
        return ''; // Must be a text node to provide a context.
      }

      return caret.node.textContent.substr(caret.offset);
    }

    getText(): string {
      return this.root.innerText;
    }

    deleteCharsBeforeCaret(dn: number) {
      if(!this.hasSelection() || dn <= 0) {
        return;
      }

      let start = this.getCarets().start;

      if(start.node.nodeType != 3) {
        console.warn("Deletion of characters requested without available context!");
        return; // No context to delete characters from.
      }

      let range = this.root.ownerDocument.createRange();
      let dnOffset = start.offset - start.node.nodeValue.substr(0, start.offset)._kmwSubstr(-dn).length;

      range.setStart(start.node, dnOffset);
      range.setEnd(start.node, start.offset);

      this.adjustDeadkeys(-dn);
      range.deleteContents();
      // No need to reposition the caret - the DOM will auto-move the selection accordingly, since
      // we didn't use the selection to delete anything.
    }

    insertTextBeforeCaret(s: string) {
      if(!this.hasSelection()) {
        return;
      }

      let start = this.getCarets().start;
      let delta = s._kmwLength();
      let Lsel = this.root.ownerDocument.getSelection();

      if(delta == 0) {
        return;
      }

      this.adjustDeadkeys(delta);

      // While Selection.extend() is really nice for this, IE doesn't support it whatsoever.
      // However, IE (11, at least) DOES support setting selections via ranges, so we can still
      // manage the caret properly.
      let finalCaret = this.root.ownerDocument.createRange();

      if(start.node.nodeType == 3) {
        let textStart = <Text> start.node;
        textStart.insertData(start.offset, s);
        finalCaret.setStart(textStart, start.offset + s.length);
      } else {
        // Create a new text node - empty control
        var n = start.node.ownerDocument.createTextNode(s);

        let range = this.root.ownerDocument.createRange();
        range.setStart(start.node, s.length);
        range.collapse(true);
        range.insertNode(n);
      }

      finalCaret.collapse(true);
      Lsel.removeAllRanges();
      try {
        Lsel.addRange(finalCaret);
      } catch(e) {
        // Chrome (through 4.0 at least) throws an exception because it has not synchronised its content with the selection.
        // scrollIntoView synchronises the content for selection
        start.node.parentElement.scrollIntoView();
        Lsel.addRange(finalCaret);
      }
      Lsel.collapseToEnd();
    }
  }
}