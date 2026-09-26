export function probe(primary: unknown) { try { throw new Error('owned'); } catch(error) { throw new Error('cleanup failed after primary', { cause: error }); } }
