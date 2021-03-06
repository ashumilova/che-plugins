/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.svn.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.svn.shared.AddRequest;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputResponse;
import org.eclipse.che.ide.ext.svn.shared.CLIOutputWithRevisionResponse;
import org.eclipse.che.ide.ext.svn.shared.CleanupRequest;
import org.eclipse.che.ide.ext.svn.shared.CommitRequest;
import org.eclipse.che.ide.ext.svn.shared.CopyRequest;
import org.eclipse.che.ide.ext.svn.shared.LockRequest;
import org.eclipse.che.ide.ext.svn.shared.MoveRequest;
import org.eclipse.che.ide.ext.svn.shared.RemoveRequest;
import org.eclipse.che.ide.ext.svn.shared.ResolveRequest;
import org.eclipse.che.ide.ext.svn.shared.RevertRequest;
import org.eclipse.che.ide.ext.svn.shared.SaveCredentialsRequest;
import org.eclipse.che.ide.ext.svn.shared.ShowDiffRequest;
import org.eclipse.che.ide.ext.svn.shared.ShowLogRequest;
import org.eclipse.che.ide.ext.svn.shared.StatusRequest;
import org.eclipse.che.ide.ext.svn.shared.UpdateRequest;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implementation of the {@link SubversionClientService}.
 *
 * @author Jeremy Whitlock
 */
@Singleton
public class SubversionClientServiceImpl implements SubversionClientService {

    private final AsyncRequestFactory asyncRequestFactory;
    private final DtoFactory          dtoFactory;
    private final AsyncRequestLoader  loader;
    private final String              baseHttpUrl;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;

    /**
     * Constructor.
     */
    @Inject
    public SubversionClientServiceImpl(@Named("restContext") String restContext,
                                       @Named("workspaceId") String workspaceId,
                                       final AsyncRequestFactory asyncRequestFactory,
                                       final DtoFactory dtoFactory,
                                       final AsyncRequestLoader loader,
                                       final DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoFactory = dtoFactory;
        this.loader = loader;
        this.baseHttpUrl = restContext + "/svn/" + workspaceId;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    @Override
    public void add(@NotNull final String projectPath, final List<String> paths, final String depth,
                    final boolean addIgnored, final boolean addParents, final boolean autoProps,
                    final boolean noAutoProps,
                    final AsyncRequestCallback<CLIOutputResponse> callback) {
        final AddRequest request =
                dtoFactory.createDto(AddRequest.class)
                          .withAddIgnored(addIgnored)
                          .withAddParents(addParents)
                          .withDepth(depth)
                          .withPaths(paths)
                          .withProjectPath(projectPath)
                          .withAutoProps(autoProps)
                          .withNotAutoProps(noAutoProps);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/add", request).loader(loader).send(callback);
    }

    @Override
    public void revert(String projectPath, List<String> paths, final String depth, AsyncRequestCallback<CLIOutputResponse> callback) {
        final RevertRequest request = dtoFactory.createDto(RevertRequest.class).withProjectPath(projectPath).withPaths(paths)
                                                .withDepth(depth);
        asyncRequestFactory.createPostRequest(baseHttpUrl + "/revert", request).loader(loader).send(callback);
    }

    @Override
    public void copy(@NotNull String projectPath, String source, String destination, String comment,
                     AsyncRequestCallback<CLIOutputResponse> callback) {
        final CopyRequest request =
                dtoFactory.createDto(CopyRequest.class)
                          .withProjectPath(projectPath)
                          .withSource(source)
                          .withDestination(destination)
                          .withComment(comment);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/copy", request).loader(loader).send(callback);
    }

    public void remove(@NotNull final String projectPath, final List<String> paths,
                       final AsyncRequestCallback<CLIOutputResponse> callback) {
        final RemoveRequest request =
                dtoFactory.createDto(RemoveRequest.class)
                          .withPaths(paths)
                          .withProjectPath(projectPath);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/remove", request).loader(loader).send(callback);
    }

    @Override
    public void status(@NotNull final String projectPath, final List<String> paths, final String depth,
                       final boolean ignoreExternals, final boolean showIgnored, final boolean showUpdates,
                       final boolean showUnversioned, final boolean verbose, final List<String> changeLists,
                       final AsyncRequestCallback<CLIOutputResponse> callback) {
        final StatusRequest request =
                dtoFactory.createDto(StatusRequest.class)
                          .withVerbose(verbose)
                          .withChangeLists(changeLists)
                          .withDepth(depth)
                          .withIgnoreExternals(ignoreExternals)
                          .withPaths(paths)
                          .withProjectPath(projectPath)
                          .withShowIgnored(showIgnored)
                          .withShowUnversioned(showUnversioned)
                          .withShowUpdates(showUpdates);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/status", request).loader(loader).send(callback);
    }

    @Override
    public void update(@NotNull final String projectPath,
                       final List<String> paths,
                       final String revision,
                       final String depth,
                       final boolean ignoreExternals,
                       final String accept,
                       final AsyncRequestCallback<CLIOutputWithRevisionResponse> callback) {
        final UpdateRequest request =
                dtoFactory.createDto(UpdateRequest.class)
                          .withProjectPath(projectPath)
                          .withPaths(paths)
                          .withRevision(revision)
                          .withDepth(depth)
                          .withIgnoreExternals(ignoreExternals)
                          .withAccept(accept);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/update", request).loader(loader).send(callback);
    }

    @Override
    public void showLog(final String projectPath,
                        final List<String> paths,
                        final String revision,
                        final AsyncRequestCallback<CLIOutputResponse> callback) {
        final String url = baseHttpUrl + "/showlog";
        final ShowLogRequest request = dtoFactory.createDto(ShowLogRequest.class)
                                                 .withProjectPath(projectPath)
                                                 .withPaths(paths)
                                                 .withRevision(revision);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);
    }

    @Override
    public void lock(final @NotNull String projectPath, final List<String> paths, final boolean force,
              final AsyncRequestCallback<CLIOutputResponse> callback) {
        final String url = baseHttpUrl + "/lock";
        final LockRequest request = dtoFactory.createDto(LockRequest.class)
                .withProjectPath(projectPath)
                .withTargets(paths)
                .withForce(force);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);
    }

    @Override
    public void unlock(final @NotNull String projectPath, final List<String> paths, final boolean force,
                final AsyncRequestCallback<CLIOutputResponse> callback) {
        final String url = baseHttpUrl + "/unlock";
        final LockRequest request = dtoFactory.createDto(LockRequest.class)
                .withProjectPath(projectPath)
                .withTargets(paths)
                .withForce(force);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);
    }

    @Override
    public void showDiff(final String projectPath,
                        final List<String> paths,
                        final String revision,
                        final AsyncRequestCallback<CLIOutputResponse> callback) {
        final String url = baseHttpUrl + "/showdiff";
        final ShowDiffRequest request = dtoFactory.createDto(ShowDiffRequest.class)
                .withProjectPath(projectPath)
                .withPaths(paths)
                .withRevision(revision);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);
    }

    @Override
    public void commit(final String projectPath, final List<String> paths, final String message,
                       final boolean keepChangeLists, final boolean keepLocks,
                       final AsyncRequestCallback<CLIOutputWithRevisionResponse> callback) {
        final String url = baseHttpUrl + "/commit";
        final CommitRequest request = dtoFactory.createDto(CommitRequest.class)
                                                .withPaths(paths)
                                                .withMessage(message)
                                                .withProjectPath(projectPath)
                                                .withKeepChangeLists(keepChangeLists)
                                                .withKeepLocks(keepLocks);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);
    }

    @Override
    public void cleanup(final String projectPath, final List<String> paths,
                        final AsyncRequestCallback<CLIOutputResponse> callback) {
        final String url = baseHttpUrl + "/cleanup";
        final CleanupRequest request = dtoFactory.createDto(CleanupRequest.class)
                                                 .withPaths(paths)
                                                 .withProjectPath(projectPath);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);
    }

    @Override
    public void showConflicts(final String projectPath, final List<String> paths, final AsyncCallback<List<String>> callback) {
        status(projectPath, paths, "infinity",
                false, // @param ignoreExternals whether or not to ignore externals (--ignore-externals)
                false, // @param showIgnored whether or not to show ignored paths (--no-ignored)
                false, // @param showUpdates whether or not to show repository updates (--show-updates)
                false, // @param showUnversioned whether or not to show unversioned paths (--quiet)
                false, // @param verbose whether or not to be verbose (--verbose)
                new ArrayList<String>(),

                new AsyncRequestCallback<CLIOutputResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class)) {
                    @Override
                    protected void onSuccess(CLIOutputResponse result) {
                        if (result != null) {
                            List<String> conflictsList = parseConflictsList(result.getOutput());
                            callback.onSuccess(conflictsList);
                        } else {
                            callback.onFailure(new Exception("showConflicts : no SvnResponse."));
                        }
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                   }
               });
    }

    protected List<String> parseConflictsList(List<String> output) {
        List<String> conflictsList = new ArrayList<String>();
        for (String line : output) {
            if (line.startsWith("C ")) {
                int lastSpaceIndex = line.lastIndexOf(' ');
                String filePathMatched = line.substring(lastSpaceIndex + 1);
                conflictsList.add(filePathMatched);
            }
        }
        return conflictsList;
    }

    @Override
    public void resolve(final String projectPath,
                        final Map<String, String> resolutions,
                        final String depth,
                        final AsyncCallback<List<String>> callback) {
        final String url = baseHttpUrl + "/resolve";
        final ResolveRequest request = dtoFactory.createDto(ResolveRequest.class)
                                                 .withProjectPath(projectPath)
                                                 .withConflictResolutions(resolutions)
                                                 .withDepth(depth);
        asyncRequestFactory.createPostRequest(url, request).loader(loader)
                           .send(new AsyncRequestCallback<CLIOutputResponse>(dtoUnmarshallerFactory.newUnmarshaller(CLIOutputResponse.class)) {

                               @Override
                               protected void onSuccess(CLIOutputResponse result) {
                                   if (result != null) {
                                       callback.onSuccess(result.getOutput());
                                   } else {
                                       callback.onFailure(new Exception("resolve : no SvnResponse."));
                                   }
                               }

                               @Override
                               protected void onFailure(Throwable exception) {
                                   callback.onFailure(exception);
                               }
                           });
    }
    @Override
    public void saveCredentials(final String repositoryUrl, final String username, final String password,
                                final AsyncRequestCallback<Void> callback) {
        final String url = baseHttpUrl + "/saveCredentials";
        final SaveCredentialsRequest request = dtoFactory.createDto(SaveCredentialsRequest.class)
                                                         .withUsername(username)
                                                         .withPassword(password)
                                                         .withRepositoryUrl(repositoryUrl);
        asyncRequestFactory.createPostRequest(url, request).loader(loader).send(callback);

    }

    @Override
    public void move(@NotNull String projectPath, List<String> source, String destination, String comment,
                      AsyncRequestCallback<CLIOutputResponse> callback) {
        final MoveRequest request =
                dtoFactory.createDto(MoveRequest.class)
                          .withProjectPath(projectPath)
                          .withSource(source)
                          .withDestination(destination)
                          .withComment(comment);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/move", request).loader(loader).send(callback);
    }
}
