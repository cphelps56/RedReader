/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.reddit;

import android.net.Uri;
import android.util.Log;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.listingcontrollers.PostListingController;

import java.util.ArrayList;
import java.util.List;

public class RedditURLContainer {

	public enum PathType {
		SubredditPostListURL
	}

	public final PathType type;
	private final RedditURL url;

	private RedditURLContainer(PathType type, RedditURL url) {
		this.type = type;
		this.url = url;
	}

	public static RedditURLContainer parse(Uri uri) {

		{
			final SubredditPostListURL subredditPostListURL = SubredditPostListURL.parse(uri);
			if(subredditPostListURL != null) {
				return new RedditURLContainer(PathType.SubredditPostListURL, subredditPostListURL);
			}
		}

		return null;
	}

	public SubredditPostListURL getAsSubredditPostListURL() {
		return (SubredditPostListURL)url;
	}

	public static abstract class RedditURL {
		// TODO .json generate

		public abstract Uri generateUri();
	}

	public static class SubredditPostListURL extends RedditURL {

		public enum Type {
			FRONTPAGE, ALL, SUBREDDIT, SUBREDDIT_COMBINATION, ALL_SUBTRACTION
		}

		public final Type type;
		public final String subreddit;

		public final PostListingController.Sort order;
		public final Integer limit;
		public final String before, after;

		private SubredditPostListURL(Type type, String subreddit, PostListingController.Sort order, Integer limit, String before, String after) {
			this.type = type;
			this.subreddit = subreddit;
			this.order = order;
			this.limit = limit;
			this.before = before;
			this.after = after;
		}

		public SubredditPostListURL after(String newAfter) {
			return new SubredditPostListURL(type, subreddit, order, limit, before, newAfter);
		}

		public SubredditPostListURL sort(PostListingController.Sort newOrder) {
			return new SubredditPostListURL(type, subreddit, newOrder, limit, before, after);
		}

		private static PostListingController.Sort getOrder(String sort, String t) {

			sort = sort.toLowerCase();
			t = t != null ? t.toLowerCase() : null;

			if(sort.equals("hot")) {
				return PostListingController.Sort.HOT;
			} else if(sort.equals("new")) {
				return PostListingController.Sort.NEW;
			} else if(sort.equals("controversial")) {
				return PostListingController.Sort.CONTROVERSIAL;
			} else if(sort.equals("rising")) {
				return PostListingController.Sort.RISING;
			} else if(sort.equals("top")) {

				if(t == null)				return PostListingController.Sort.TOP_ALL;
				else if(t.equals("all"))	return PostListingController.Sort.TOP_ALL;
				else if(t.equals("hour"))	return PostListingController.Sort.TOP_HOUR;
				else if(t.equals("day"))	return PostListingController.Sort.TOP_DAY;
				else if(t.equals("week"))	return PostListingController.Sort.TOP_WEEK;
				else if(t.equals("month"))	return PostListingController.Sort.TOP_MONTH;
				else if(t.equals("year"))	return PostListingController.Sort.TOP_YEAR;
				else						return PostListingController.Sort.TOP_ALL;

			} else {
				return null;
			}
		}


		@Override
		public Uri generateUri() {

			Uri.Builder builder = new Uri.Builder();
			builder.scheme(Constants.Reddit.getScheme()).authority(Constants.Reddit.getDomain());

			switch(type) {

				case FRONTPAGE:
					builder.encodedPath("/");
					break;

				case ALL:
					builder.encodedPath("/r/all");
					break;

				case SUBREDDIT:
				case SUBREDDIT_COMBINATION:
				case ALL_SUBTRACTION:
					builder.encodedPath("/r/");
					builder.appendPath(subreddit);
					break;
			}

			if(order != null) {
				switch(order) {

					case HOT:
						builder.appendEncodedPath("hot");
						break;

					case NEW:
						builder.appendEncodedPath("new");
						break;

					case RISING:
						builder.appendEncodedPath("rising");
						break;

					case CONTROVERSIAL:
						builder.appendEncodedPath("controversial");
						break;

					case TOP_HOUR:
						builder.appendEncodedPath("top");
						builder.appendQueryParameter("t", "hour");
						break;

					case TOP_DAY:
						builder.appendEncodedPath("top");
						builder.appendQueryParameter("t", "day");
						break;

					case TOP_WEEK:
						builder.appendEncodedPath("top");
						builder.appendQueryParameter("t", "week");
						break;

					case TOP_MONTH:
						builder.appendEncodedPath("top");
						builder.appendQueryParameter("t", "month");
						break;

					case TOP_YEAR:
						builder.appendEncodedPath("top");
						builder.appendQueryParameter("t", "year");
						break;

					case TOP_ALL:
						builder.appendEncodedPath("top");
						builder.appendQueryParameter("t", "all");
						break;
				}
			}

			if(before != null) {
				builder.appendQueryParameter("before", before);
			}

			if(after != null) {
				builder.appendQueryParameter("after", after);
			}

			if(limit != null) {
				builder.appendQueryParameter("limit", String.valueOf(limit));
			}

			builder.appendEncodedPath(".json");

			return builder.build();
		}

		public static SubredditPostListURL parse(final Uri uri) {

			Integer limit = null;
			String before = null, after = null;

			for(final String parameterKey : General.getUriQueryParameterNames(uri)) {

				if(parameterKey.equalsIgnoreCase("after")) {
					after = uri.getQueryParameter(parameterKey);

				} else if(parameterKey.equalsIgnoreCase("before")) {
					before = uri.getQueryParameter(parameterKey);

				} else if(parameterKey.equalsIgnoreCase("limit")) {
					try {
						limit = Integer.parseInt(uri.getQueryParameter(parameterKey));
					} catch(Throwable ignored) {}

				} else {
					Log.e("SubredditPostListURL", String.format("Unknown query parameter '%s'", parameterKey));
				}
			}

			final String[] pathSegments;
			{
				final List<String> pathSegmentsList = uri.getPathSegments();

				final ArrayList<String> pathSegmentsFiltered = new ArrayList<String>(pathSegmentsList.size());
				for(String segment : pathSegmentsList) {

					while(segment.toLowerCase().endsWith(".json") || segment.toLowerCase().endsWith(".xml")) {
						segment = segment.substring(0, segment.lastIndexOf('.'));
					}

					if(segment.length() > 0) {
						pathSegmentsFiltered.add(segment);
					}
				}

				pathSegments = pathSegmentsFiltered.toArray(new String[pathSegmentsFiltered.size()]);
			}

			final PostListingController.Sort order;
			if(pathSegments.length > 0) {
				order = getOrder(pathSegments[pathSegments.length - 1], uri.getQueryParameter("t"));
			} else {
				order = null;
			}

			switch(pathSegments.length) {
				case 0:
					return new SubredditPostListURL(Type.FRONTPAGE, null, PostListingController.Sort.HOT, limit, before, after);

				case 1: {
					if(order != null) {
						return new SubredditPostListURL(Type.FRONTPAGE, null, order, limit, before, after);
					} else {
						return null;
					}
				}

				case 2:
				case 3: {

					if(!pathSegments[0].equals("r")) return null;

					final String subreddit = pathSegments[1];

					if(subreddit.equals("all")) {

						if(pathSegments.length == 2) {
							return new SubredditPostListURL(Type.ALL, null, PostListingController.Sort.HOT, limit, before, after);

						} else if(order != null) {
							return new SubredditPostListURL(Type.ALL, null, order, limit, before, after);

						} else {
							return null;
						}

					} else if(subreddit.matches("all(\\-\\w+)+")) {

						if(pathSegments.length == 2) {
							return new SubredditPostListURL(Type.ALL_SUBTRACTION, subreddit, PostListingController.Sort.HOT, limit, before, after);

						} else if(order != null) {
							return new SubredditPostListURL(Type.ALL_SUBTRACTION, subreddit, order, limit, before, after);

						} else {
							return null;
						}

					} else if(subreddit.matches("\\w+(\\+\\w+)+")) {

						if(pathSegments.length == 2) {
							return new SubredditPostListURL(Type.SUBREDDIT_COMBINATION, subreddit, PostListingController.Sort.HOT, limit, before, after);

						} else if(order != null) {
							return new SubredditPostListURL(Type.SUBREDDIT_COMBINATION, subreddit, order, limit, before, after);

						} else {
							return null;
						}

					} else if(subreddit.matches("\\w+")) {

						if(pathSegments.length == 2) {
							return new SubredditPostListURL(Type.SUBREDDIT, subreddit, PostListingController.Sort.HOT, limit, before, after);

						} else if(order != null) {
							return new SubredditPostListURL(Type.SUBREDDIT, subreddit, order, limit, before, after);

						} else {
							return null;
						}

					} else {
						return null;
					}
				}

				default:
					return null;
			}
		}
	}
}
