package com.qrparking.app

import org.json.JSONObject

/**
 * JavaScript que se inyecta en la página del centro comercial.
 */
object FillScript {

    /**
     * Se ejecuta al terminar de cargar cada página: recuerda el último campo de texto
     * que el usuario tocó, para poder escribir ahí aunque pierda el foco al pulsar el botón.
     */
    const val TRACK_FOCUS = """
        (function () {
          if (window.__qrParkingTracking) return;
          window.__qrParkingTracking = true;
          document.addEventListener('focusin', function (e) {
            var t = e.target;
            if (t && (t.tagName === 'INPUT' || t.tagName === 'TEXTAREA')) window.__qrParkingLastField = t;
          }, true);
        })();
    """

    /**
     * Escribe [code] en el campo adecuado y, si [submit] es true, envía el formulario.
     * Devuelve "OK" o "NOT_FOUND".
     */
    fun fill(code: String, selector: String, submit: Boolean): String {
        val args = "${JSONObject.quote(code)}, ${JSONObject.quote(selector)}, $submit"
        return """
            (function (code, selector, submit) {
              function isField(el) {
                if (!el || el.disabled || el.readOnly) return false;
                if (el.tagName === 'TEXTAREA') return true;
                if (el.tagName !== 'INPUT') return false;
                return !/^(hidden|submit|button|checkbox|radio|file|image|reset|password|range|color)$/i.test(el.type || '');
              }
              function visible(el) {
                return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
              }
              function firstVisibleField(doc) {
                var all = doc.querySelectorAll('input, textarea');
                for (var i = 0; i < all.length; i++) {
                  if (isField(all[i]) && visible(all[i])) return all[i];
                }
                return null;
              }

              var el = null;
              if (selector) { try { el = document.querySelector(selector); } catch (e) {} }
              if (!isField(el) && isField(document.activeElement)) el = document.activeElement;
              var last = window.__qrParkingLastField;
              if (!isField(el) && last && last.isConnected && visible(last)) el = last;
              if (!isField(el)) el = firstVisibleField(document);
              if (!isField(el)) {
                // Formularios dentro de iframes del mismo dominio.
                var frames = document.querySelectorAll('iframe');
                for (var f = 0; f < frames.length && !isField(el); f++) {
                  try { el = firstVisibleField(frames[f].contentDocument); } catch (e) {}
                }
              }
              if (!isField(el)) return 'NOT_FOUND';

              el.focus();
              // Usar el setter nativo para que frameworks como React/Angular/Vue detecten el cambio.
              var win = el.ownerDocument.defaultView || window;
              var proto = el.tagName === 'TEXTAREA' ? win.HTMLTextAreaElement.prototype : win.HTMLInputElement.prototype;
              var desc = Object.getOwnPropertyDescriptor(proto, 'value');
              if (desc && desc.set) desc.set.call(el, code); else el.value = code;
              el.dispatchEvent(new Event('input', { bubbles: true }));
              el.dispatchEvent(new Event('change', { bubbles: true }));
              el.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true, key: code.slice(-1) }));

              if (submit) {
                setTimeout(function () {
                  var opts = { bubbles: true, cancelable: true, key: 'Enter', code: 'Enter', keyCode: 13, which: 13 };
                  var notCancelled = el.dispatchEvent(new KeyboardEvent('keydown', opts));
                  el.dispatchEvent(new KeyboardEvent('keyup', opts));
                  var form = el.form;
                  if (!notCancelled) return;
                  if (form) {
                    var btn = form.querySelector('button[type=submit], input[type=submit], button:not([type])');
                    if (btn) btn.click();
                    else if (form.requestSubmit) form.requestSubmit();
                    else form.submit();
                  }
                }, 150);
              }
              return 'OK';
            })($args);
        """.trimIndent()
    }

    /**
     * Rellena usuario y contraseña en el formulario de inicio de sesión, sin enviarlo.
     * Si el formulario aún no aparece (páginas que cargan con JavaScript), reintenta unos segundos.
     */
    fun login(user: String, pass: String): String {
        val args = "${JSONObject.quote(user)}, ${JSONObject.quote(pass)}"
        return """
            (function (user, pass) {
              function visible(el) {
                return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
              }
              function usable(el) { return el && !el.disabled && !el.readOnly && visible(el); }
              function setValue(el, v) {
                if (el.value === v) return;
                var win = el.ownerDocument.defaultView || window;
                var desc = Object.getOwnPropertyDescriptor(win.HTMLInputElement.prototype, 'value');
                if (desc && desc.set) desc.set.call(el, v); else el.value = v;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
              }
              function tryFill() {
                var pw = null, all = document.querySelectorAll('input[type=password]');
                for (var i = 0; i < all.length; i++) if (usable(all[i])) { pw = all[i]; break; }
                if (!pw) return false;
                // Campo de usuario: el último campo de texto que aparece antes de la contraseña.
                var userEl = null, inputs = (pw.form || document).querySelectorAll('input');
                for (var j = 0; j < inputs.length; j++) {
                  var el = inputs[j];
                  if (el === pw) break;
                  if (/^(text|email|tel|number|)$/i.test(el.getAttribute('type') || '') && usable(el)) userEl = el;
                }
                if (userEl && user) setValue(userEl, user);
                if (pass) setValue(pw, pass);
                return true;
              }
              if (tryFill()) return 'OK';
              var tries = 0;
              var timer = setInterval(function () { if (tryFill() || ++tries > 20) clearInterval(timer); }, 500);
              return 'WAITING';
            })($args);
        """.trimIndent()
    }
}
