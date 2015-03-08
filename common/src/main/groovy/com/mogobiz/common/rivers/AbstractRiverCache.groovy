/**
 * 
 */
package com.mogobiz.common.rivers

/**
 * @author stephane.manciot@ebiznext.com
 *
 */
abstract class AbstractRiverCache< T > {

	/**
	 *
	 */
	static final long                                          DEFAULT_MAX_OBJECTS_IN_CACHE

	/**
	 *
	 */
	static final long                                          DEFAULT_EXPIRE_TIME_IN_SECONDS

	/**
	 *
	 */
	private static final Collection < AbstractRiverCache < ? > > CACHES

	/**
	 * nombre d'entrées dans la map
	 */
	private long                                                       count

	/**
	 * indice correspondant à l'entrée la plus anciennement ajoutée au cache et
	 * par conséquent première candidate à être retirée du cache en cas de
	 * dépassement de la limite du nombre d'entrées dans le cache
	 */
	private long                                                       lastRemoved

	/**
	 *
	 */
	private Map < String, CacheEntry >                                 cache  =
	new HashMap < String, CacheEntry >()

	/**
	 *
	 */
	private Map < Long, String >                                       lruMap =
	new HashMap < Long, String >()

	/**
	 * nombre maximum d'objets pouvant être ajoutés au cache
	 */
	private long                                                       maxObjects

	/**
	 * moment dans la journée en secondes auquel l'entrée expire
	 */
	private long                                                       expireTime

	static {
		DEFAULT_MAX_OBJECTS_IN_CACHE = 1000
		DEFAULT_EXPIRE_TIME_IN_SECONDS = 24 * 3600
		CACHES = new ArrayList < AbstractRiverCache < ? > >()
	}

	/**
	 *
	 */
	AbstractRiverCache() {
		this.count = 0
		this.lastRemoved = 0
		this.maxObjects = DEFAULT_MAX_OBJECTS_IN_CACHE
		this.expireTime = DEFAULT_EXPIRE_TIME_IN_SECONDS
		CACHES.add(this)
	}

	/**
	 * initialisation des paramètres du cache. Doit être appelé dans le
	 * constructeur de la classe dérivée.
	 * 
	 * @return
	 */
	protected void initVars(long maxObjects = DEFAULT_MAX_OBJECTS_IN_CACHE, long expireTime = DEFAULT_EXPIRE_TIME_IN_SECONDS) {
		this.maxObjects = maxObjects
		this.expireTime = expireTime
	}

	/**
	 * purge cache
	 */
	protected void purge() {
		cache.clear()
		lruMap.clear()
		count = 0
		lastRemoved = 0
	}

	/**
	 * purge all caches
	 */
	public static void purgeAll() {
		for (Iterator < AbstractRiverCache < ? > > iterator = CACHES.iterator(); iterator
		.hasNext();) {
			AbstractRiverCache < ? > cache = iterator.next()
			cache.purge()
		}
	}

	/**
	 * @param key
	 *            - clef
	 * @return entrée correspondante dans le cache
	 */
	public T get(String key) {
		if (maxObjects == 0)
			return null
		CacheEntry entry = cache.get(key)
		if (entry == null) {
			return null
		}
		else if (entry.isExpired()) {
			cache.remove(key)
			return null
		}
		else {
			entry.setLastAccessTime(Calendar.getInstance().getTimeInMillis())
			return entry.getValue()
		}
	}

    /**
     *
     * @return toutes les entrées présentes au sein du cache
     */
    public Collection<T> getAll() {
        cache.keySet().collect {
            def value = get(it)
            value ?: []
        }.flatten()
    }

	/**
	 * @param key
	 *            - clef
	 * @param value
	 *            - entrée
	 */
	public void put(String key, T value) {
		if (maxObjects == 0)
			return
		count++
		if (count > maxObjects) {
			String keyToRemove = lruMap.get(lastRemoved)
			lruMap.remove(lastRemoved)
			lastRemoved++
			cache.remove(keyToRemove)
		}
		CacheEntry entry = new CacheEntry(expireTime, key, value)
		cache.put(key, entry)
		lruMap.put(count, key)
	}

	/**
	 * @version $Id $
	 * 
	 */
	class CacheEntry {
		/**
		 * clef au sein de la map
		 */
		private String key

		/**
		 * entrée
		 */
		private T      value

		/**
		 * heure à laquelle cette entrée a été ajoutée au cache
		 */
		private long   creationTime

		/**
		 * jour auquel cette entrée a été ajoutée au cache
		 */
		private int    creationDay

		/**
		 * 
		 */
		private long   lastAccessTime

		private final long expireTime
		/**
		 * @return the lastAccessTime
		 */
		protected long getLastAccessTime() {
			return lastAccessTime
		}

		/**
		 * @return true si l'entrée correspondante a expiré et doit être retirée
		 *         du cache
		 */
		public boolean isExpired() {
			Calendar now = Calendar.getInstance()
			long accessTime =
					now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60
			+ now.get(Calendar.SECOND)
			int accessDay = now.get(Calendar.DAY_OF_YEAR)
			return accessDay != creationDay	|| (creationTime < expireTime && accessTime >= expireTime)
		}

		/**
		 * @param key
		 *            - clef dans le cache
		 * @param value
		 *            - entrée correspondante
		 */
		public CacheEntry(long expireTime, String key, T value) {
			Calendar now = Calendar.getInstance()
			creationTime =
					now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60
			+ now.get(Calendar.SECOND)
			creationDay = now.get(Calendar.DAY_OF_YEAR)
			this.key = key
			this.value = value
			this.expireTime = expireTime
			lastAccessTime = now.getTimeInMillis()
		}

		/**
		 * @return clef
		 */
		public String getKey() {
			return this.key
		}

		/**
		 * @return entrée
		 */
		public T getValue() {
			return this.value as T
		}

		/**
		 * @param timeInMillis
		 *            - timestamp
		 */
		public void setLastAccessTime(long timeInMillis) {
			this.lastAccessTime = timeInMillis
		}
	}
}
