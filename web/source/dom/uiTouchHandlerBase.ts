namespace com.keyman.dom {
  export abstract class UITouchHandlerBase<Target extends HTMLElement> {
    private rowClassMatch: string;
    private selectedTargetMatch: string;
    private baseElement: HTMLElement;

    private touchX: number;
    private touchY: number;
    private touchCount: number;

    private currentTarget: Target;
    private pendingTarget: Target;
    private popupBaseTarget: Target;

    constructor(baseElement: HTMLElement, rowClassMatch: string, selectedTargetMatch: string) {
      this.baseElement = baseElement;
      this.rowClassMatch = rowClassMatch;
      this.selectedTargetMatch = selectedTargetMatch;
    }

    /**
     * Finds the internally-preferred target element or submenu target element.
     * @param e The DOM element that actually received the touch event.
     * May be parent, child, or the actually-desired element itself.
     */
    abstract findTargetFrom(e: HTMLElement): Target;

    /**
     * Highlights the target element as visual feedback representing
     * a pending touch.
     * @param t The `Target` to highlight
     * @param state `true` to apply highlighting, `false` to remove it.
     */
    protected abstract highlight(t: Target, state: boolean): void;

    /**
     * Called whenever the touch-handling analysis determines that the Target has been selected
     * @param t The `Target` to activate/execute.
     */
    protected abstract select(t: Target): void;

    /**
     * Requests info on whether or not the indicated `Target` has subkeys or a submenu.
     * @param t A `Target`.
     */
    protected abstract hasSubmenu(t: Target): boolean;

    /**
     * Indicates that the user is maintaining a `Touch` on the specified `Target`. 
     * Popups and-or longpress menus may be appropriate.
     * @param t The `Target` being held.
     */
    protected abstract hold(t: Target): void;

    /**
     * Signals that any popup elements (previews, subkey views, etc) should be cancelled.
     */
    protected abstract clearHolds(): void;

    /**
     * Requests a boolean indicating whether or not the UI is currently displaying any input-blocking popup elements.
     * Embedded mode should return `true` when the app is displaying popup menus.
     */
    protected abstract hasModalPopup(): boolean;

    /**
     * Designed to support highlighting of prepended base keys on phone form-factor subkey menus.
     * @param target The base element with a potential subkey menu alias.
     * @returns The aliased submenu version of the `Target`, or the original `Target` if no alias exists.
     */
    protected abstract dealiasSubTarget(target: Target): Target;

    /**
     * Should return true whenever a 'native'-mode submenu (or subkey) display is active.
     */
    protected abstract isSubmenuActive(): boolean;

    /**
     * For 'native' mode - requests that the submenu for the indicated `Target` be instantly displayed.
     * @param target The base element with a potential submenu
     */
    protected abstract displaySubmenuFor(target: Target);

    /**
     * Identify the key nearest to (but NOT under) the touch point if at the end of a key row,
     * but return null more than about 0.6 key width from the nearest key.
     *
     *  @param  {Event}   e   touch event
     *  @param  {Object}  t   HTML object at touch point
     *  @return {Object}      nearest key to touch point
     *
     **/
    findNearestTarget(e: TouchEvent, t: HTMLElement): Target {
      if((!e) || (typeof e.changedTouches == 'undefined')
        || (e.changedTouches.length == 0)) {
        return null;
      }

      // Get touch point on screen
      var x = e.changedTouches[0].pageX;

      // Get the UI row beneath touch point (SuggestionBanner div, 'kmw-key-row' if OSK, ...)
      while(t && t.className !== undefined && t.className.indexOf(this.rowClassMatch) < 0) {
        t = <HTMLElement> t.parentNode;
      }
      if(!t) {
        return null;
      }

      // Find minimum distance from any key
      var k: number, bestMatch=0, dx: number, dxMax=24, dxMin=100000, x1: number, x2: number;
      for(k = 0; k < t.childNodes.length; k++) {
        let childNode = t.childNodes[k] as HTMLElement;
        /* // If integrating with the OSK, replace with an 'isInvalidTarget' check.
        if(childNode.className !== undefined && childNode.className.indexOf('key-hidden') >= 0) {
          continue;
        }*/
        x1 = childNode.offsetLeft;
        x2 = x1 + childNode.offsetWidth;
        dx = x1 - x;
        if(dx >= 0 && dx < dxMin) {
          bestMatch = k;
          dxMin = dx;
        }
        dx = x - x2;
        if(dx >= 0 && dx < dxMin) {
          bestMatch = k;
          dxMin = dx;
        }
      }

      if(dxMin < 100000) {
        t = <HTMLElement> t.childNodes[bestMatch];
        x1 = t.offsetLeft;
        x2 = x1 + t.offsetWidth;

        // Limit extended touch area to the larger of 0.6 of the potential target's width and 24 px
        if(t.offsetWidth > 40) {
          dxMax = 0.6 * t.offsetWidth;
        }

        if(((x1 - x) >= 0 && (x1 - x) < dxMax) || ((x - x2) >= 0 && (x - x2) < dxMax)) {
          // If integrating with the OSK, should probably use `findTargetFrom`.
          // The OSK would want this (key-square) element's child.
          return <Target> t;
        }
      }
      return null;
    }

    touchStart(e: TouchEvent) {
      // Determine the selected Target, manage state.
      this.currentTarget = this.findTargetFrom(e.changedTouches[0].target as HTMLElement);
      this.touchX = e.changedTouches[0].pageX;
      this.touchY = e.changedTouches[0].pageY;

      // If popup stuff, immediately return. 
      
      this.touchCount = e.touches.length;

      // If option should not be selectable, how do we re-target?
      // (If/when the OSK is refactored to use this, we'll need appropriate code here.)
      // Probably a 'isInvalidTarget()' and 'nearestTarget()'.

      if(!this.currentTarget) {
        // Find nearest target.
        this.currentTarget = this.findNearestTarget(e, <HTMLElement> e.changedTouches[0].target)
      }

      // Still no appropriate target?  Reject the touch event.
      if(!this.currentTarget) {
        return;
      }

      // Alright, Target acquired!  Now to use it:

      // Highlight the touched key
      this.highlight(this.currentTarget,true);

      // If used by the OSK, the special function keys need immediate action
      // Add a `checkForImmediates()` to facilitate this.
      if(this.pendingTarget) {
        this.highlight(this.pendingTarget, false);
        this.select(this.pendingTarget);
        this.clearHolds();
        // Decrement the number of unreleased touch points to prevent
        // sending the keystroke again when the key is actually released
        this.touchCount--;
      } else {
        // If this key has subkey, start timer to display subkeys after delay, set up release
        this.hold(this.currentTarget);
      }
      this.pendingTarget = this.currentTarget;
    }

    touchEnd(e: TouchEvent): void {
      // Prevent incorrect multi-touch behaviour if native or device popup visible
      let t = this.currentTarget;

      if(this.isSubmenuActive() || this.hasModalPopup()) {
        // Ignore release if a multiple touch
        if(e.touches.length > 0) {
          return;
        }

        // Cancel (but do not execute) pending key if neither a popup key or the base key
        if((t == null) || ((t.id.indexOf('popup') < 0) && (t.id != this.popupBaseTarget.id))) {
          this.highlight(this.pendingTarget,false);
          this.clearHolds();
          this.pendingTarget = null;
        }
      }

      // Test if moved off screen (effective release point must be corrected for touch point horizontal speed)
      // This is not completely effective and needs some tweaking, especially on Android
      var x = e.changedTouches[0].pageX;
      var beyondEdge = ((x < 2 && this.touchX > 5) || (x > window.innerWidth - 2 && this.touchX < window.innerWidth - 5));

      // Save then decrement current touch count
      var tc=this.touchCount;
      if(this.touchCount > 0) {
        this.touchCount--;
      }

      // Process and clear highlighting of pending target
      if(this.pendingTarget) {
        this.highlight(this.pendingTarget,false);

        // Output character unless moved off key
        if(this.pendingTarget.className.indexOf('hidden') < 0 && tc > 0 && !beyondEdge) {
          this.select(this.pendingTarget);
        }
        this.clearHolds();
        this.pendingTarget = null;
        // Always clear highlighting of current target on release (multi-touch)
      } else {
        var tt = e.changedTouches[0];
        t = this.findTargetFrom(tt.target as HTMLElement);
        if(!t) {
          var t1 = document.elementFromPoint(tt.clientX,tt.clientY);
          t = this.findNearestTarget(e, <HTMLElement> t1);
        }

        if(t) {
          this.highlight(t,false);
        }
      }
    }

    /**
     * OSK touch move event handler
     *
     *  @param  {Event} e   touch move event object
     *
     **/
    touchMove(e: TouchEvent): void {
      let keyman = com.keyman.singleton;
      let util = keyman.util;

      e.preventDefault();
      e.cancelBubble=true;

      if(typeof e.stopImmediatePropagation == 'function') {
        e.stopImmediatePropagation();
      } else if(typeof e.stopPropagation == 'function') {
        e.stopPropagation();
      }

      // Do not attempt to support reselection of target key for overlapped keystrokes
      if(e.touches.length > 1 || this.touchCount == 0) {
        return;
      }

      // Get touch position
      var x=typeof e.touches == 'object' ? e.touches[0].clientX : e.clientX,
          y=typeof e.touches == 'object' ? e.touches[0].clientY : e.clientY;

      // Move target key and highlighting
      var t1 = <HTMLElement> document.elementFromPoint(x,y),
          key0 = this.pendingTarget,
          key1 = this.findTargetFrom(t1);  // For the OSK, this ALSO gets subkeys.

      // If option should not be selectable, how do we re-target?
      // (If/when the OSK is refactored to use this, we'll need appropriate code here.)
      // Probably a 'isInvalidTarget()' and 'nearestTarget()'.

      // Find the nearest key to the touch point if not on a visible key
      if(!key1) {
        key1 = this.findNearestTarget(e,t1);
      }

      // Do not move over keys if device popup visible
      if(this.hasModalPopup()) {
        if(key1 == null) {
          if(key0) {
            this.highlight(key0,false);
          }
          this.pendingTarget=null;
        } else {
          if(key1 == this.popupBaseTarget) {
            if(!util.hasClass(key1, this.selectedTargetMatch)) {
              this.highlight(key1,true);
            }
            this.pendingTarget = key1;
          } else {
            if(key0) {
              this.highlight(key0,false);
            }
            this.pendingTarget = null;
          }
        }
        return;
      }

      // Use the popup duplicate of the base key if a phone with a visible popup array
      key1 = this.dealiasSubTarget(key1);

      // Identify current touch position (to manage off-key release)
      this.currentTarget = key1;

      // Clear previous key highlighting
      if(key0 && key1 && (key1.id != key0.id)) {
        this.highlight(key0,false);
      }

      // Code below directly related to subkeys should only be triggered within 'native' mode.
      // The embedded version instead passes info to the apps to produce their own subkeys in-app.

      // If popup is visible, need to move over popup, not over main keyboard
      if(key1 && this.hasSubmenu(key1)) {
        //this.highlightSubKeys(key1,x,y);

        // Native-mode: show popup keys immediately if touch moved up towards key array (KMEW-100, Build 353)
        if(!keyman.isEmbedded && (this.touchY-y > 5) && !this.isSubmenuActive()) {
          // Instantly show the submenu.
          this.displaySubmenuFor(key1);
        }

        // Once a subkey array is displayed, do not allow changing the base key.
        // Keep that array visible and accept no other options until the touch ends.
        if(key1 && key1.id.indexOf('popup') < 0 && key1 != this.popupBaseTarget) { // TODO:  reliant on 'popup' in .id
          return;
        }

        // Highlight the base key on devices that do not append it to the subkey array.
        if(key1 && key1 == this.popupBaseTarget && key1.className.indexOf(this.selectedTargetMatch) < 0) {
          this.highlight(key1,true);
        }
        // Cancel touch if moved up and off keyboard, unless popup keys visible
      } else {
        let base = this.baseElement;
        if(key0 && e.touches[0].pageY < Math.max(5, base.offsetTop - 0.25 * base.offsetHeight)) {
          this.highlight(key0,false);
          this.clearHolds();
          this.pendingTarget = null;
        }
      }

      // Replace the target key, if any, by the new target key
      // Do not replace a null target, as that indicates the key has already been released
      if(key1 && this.pendingTarget) {
        this.pendingTarget = key1;
      }

      if(this.pendingTarget) {
        if(key1 && (key0 != key1 || key1.className.indexOf(this.selectedTargetMatch) < 0)) {
          this.highlight(key1,true);
        }
      }

      if(key0 && key1 && (key1 != key0) && (key1.id != '')) {
        //  Display the touch-hold keys (after a pause)
        this.hold(key1);
      }
    }
  }
}