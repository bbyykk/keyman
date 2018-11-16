#include <keyman/keyboardprocessor.h>
#include <iostream>
#include "processor.hpp"
#include "state.hpp"
#include <kmx/kmxtest.h>

extern const struct KMXTest_ChToVKey chToVKey[];

std::string utf16_to_utf8(std::u16string utf16_string);

namespace km {
  namespace kbp
  {

    km_kbp_status kmx_processor::validate() const {
      return m_valid ? KM_KBP_STATUS_OK : KM_KBP_STATUS_INVALID_KEYBOARD;
    }

    kmx_processor::kmx_processor(km_kbp_keyboard_attrs const & kb) : abstract_processor(kb) {
      std::filesystem::path p = kb.folder_path;
      p /= kb.id;
      p.replace_extension(".kmx");
      //char *x = wstrtostr((PKMX_WCHAR) kb.id);
      std::cout << p << std::endl;
      m_valid = (bool) kmx.Load(p.native().c_str());
      //load_keyboard();
    }
    void kmx_processor::load_keyboard() {

    }

    char VKeyToChar(KMX_UINT modifiers, KMX_UINT vk) {
      // We only map SHIFT and UNSHIFTED
      // TODO: Map CAPS LOCK correctly
      if (modifiers != 0 && modifiers != K_SHIFTFLAG) {
        return 0;
      }

      KMX_BOOL shifted = modifiers == K_SHIFTFLAG ? 1 : 0;

      if (vk == VK_SPACE) {
        // Override for space because it is the same for
        // shifted and unshifted.
        return 32;
      }

      for (int i = 0; chToVKey[i].vkey; i++) {
        if (chToVKey[i].vkey == vk && chToVKey[i].shifted == shifted) {
          return i + 32;
        }
      }
      return 0;
    }

    km_kbp_status kmx_processor::process_event(km_kbp_state *state, km_kbp_virtual_key vk, uint16_t modifier_state) {
      // Convert VK to US char
      uint16_t ch = VKeyToChar(modifier_state, vk);

      // Construct a context buffer from the items

      std::u16string ctxt;
      auto cp = state->context();
      for (auto c = cp.begin(); c != cp.end(); c++) {
        switch (c->type) {
        case KM_KBP_CT_CHAR:
          if (Uni_IsSMP(c->character)) {
            ctxt += Uni_UTF32ToSurrogate1(c->character);
            ctxt += Uni_UTF32ToSurrogate2(c->character);
          }
          else {
            ctxt += (km_kbp_cp) c->character;
          }
          break;
        case KM_KBP_CT_MARKER:
          assert(c->marker > 0);
          ctxt += UC_SENTINEL;
          ctxt += CODE_DEADKEY;
          ctxt += c->marker; 
          break;
        }
      }

      std::cout << "CONTEXT = '" << utf16_to_utf8(ctxt) << "' VK=" << vk << " mod=" << modifier_state << " ch=" << ch << std::endl;
      kmx.GetContext()->Set(ctxt.c_str());

      kmx.GetActions()->ResetQueue();
      kmx.ProcessEvent(vk, modifier_state, ch);

      state->actions.clear();

      for (auto i = 0; i < kmx.GetActions()->Length(); i++) {
        auto a = kmx.GetActions()->Get(i);
        switch (a.ItemType) {
        case QIT_CAPSLOCK:
          //TODO: add Caps Event
          //dwData = 0 == off; 1 == on
          //state->actions.emplace_back(km_kbp_action_item{ KM_KBP_IT_CAPSLOCK, {0,}, {0} });
          break;
        case QIT_VKEYDOWN:
        case QIT_VKEYUP:
        case QIT_VSHIFTDOWN:
        case QIT_VSHIFTUP:
          //TODO: eliminate??
          break;
        case QIT_CHAR:
          state->actions.emplace_back(km_kbp_action_item{ KM_KBP_IT_CHAR, {0,}, {(km_kbp_usv)a.dwData} });
          break;
        case QIT_DEADKEY:
          state->actions.emplace_back(km_kbp_action_item{ KM_KBP_IT_MARKER, {0,}, {(uintptr_t)a.dwData} });
          break;
        case QIT_BELL:
          state->actions.emplace_back(km_kbp_action_item{ KM_KBP_IT_ALERT, {0,}, {0} });
          break;
        case QIT_BACK:
          // TODO: Support deadkey backspacing: see queue CheckOutput function
          state->actions.emplace_back(km_kbp_action_item{ KM_KBP_IT_BACK, {0,}, {0} });
          break;
        case QIT_INVALIDATECONTEXT:
          // TODO: support invalidating the context
          break;
        default:
          std::cout << "Unexpected item type " << a.ItemType << ", " << a.dwData << std::endl;
          assert(false);
        }
      }

      state->actions.emplace_back(km_kbp_action_item{ KM_KBP_IT_END, {0,}, {0} });

      return 0;
    }

    constexpr km_kbp_attr const engine_attrs = {
      256,
      KM_KBP_LIB_CURRENT,
      KM_KBP_LIB_AGE,
      KM_KBP_LIB_REVISION,
      KM_KBP_TECH_KMX,
      "SIL International"
    };

    km_kbp_attr const * kmx_processor::get_attrs() const {
      //TODO
      return &engine_attrs;
    }

  } // namespace kbp
} // namespace km