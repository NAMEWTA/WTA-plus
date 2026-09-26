export function probe(primary: unknown) { try { throw new Error('owned'); } catch(error) { throw new AggregateError([primary, error], 'cleanup failed after primary', { cause: error }); } }
