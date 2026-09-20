const { test } = require('node:test');
const assert = require('node:assert/strict');
const sync = require('../save-sync');
test('saved catches survive toggles and clearing manual marks', () => {
  const saved = sync.normalize([1, 25, 493], [1, 25, 493]);
  let manual = { 25: true, 7: true };
  assert.equal(sync.toggle(manual, saved, 25), false);
  manual = {};
  assert.equal(sync.isCaught(manual, saved, 25), true);
  assert.equal(sync.isCaught(manual, saved, 7), false);
});
test('manual catches remain editable; species IDs cannot accidentally mark app-specific form IDs', () => {
  const saved = sync.normalize([1, 494, 1013, '25', null], [1, 494]);
  assert.deepEqual(saved, { 1: true, 494: true });
  const manual = {};
  assert.equal(sync.toggle(manual, saved, 1013), true);
  assert.equal(sync.isCaught(manual, saved, 1013), true);
  sync.toggle(manual, saved, 1013);
  assert.equal(sync.isCaught(manual, saved, 1013), false);
});
