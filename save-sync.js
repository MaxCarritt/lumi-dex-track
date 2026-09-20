/* Separate save-derived catches from editable manual progress. */
(function (root) {
  'use strict';
  const api = {
    normalize(ids, baseIds) {
      const supported = new Set(baseIds);
      const result = {};
      if (Array.isArray(ids)) for (const id of ids) if (Number.isInteger(id) && id >= 1 && id <= 1025 && supported.has(id)) result[id] = true;
      return result;
    },
    normalizeForms(pairs, mons) {
      const result = {};
      if (!Array.isArray(pairs)) return result;
      for (const pair of pairs) {
        if (!Array.isArray(pair) || pair.length < 2) continue;
        const m = mons.find(x => x.no === pair[0] && x.form === pair[1]);
        if (m) result[m.id] = true;
      }
      return result;
    },
    isCaught(manual, saved, id) { return !!(manual[id] || saved[id]); },
    toggle(manual, saved, id) {
      if (saved[id]) return false;
      if (manual[id]) delete manual[id]; else manual[id] = true;
      return true;
    }
  };
  if (typeof module !== 'undefined') module.exports = api;
  else root.SaveSync = api;
})(globalThis);
