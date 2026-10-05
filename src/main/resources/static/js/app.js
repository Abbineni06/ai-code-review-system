document.addEventListener('DOMContentLoaded', () => {
    if(typeof feather !== 'undefined') feather.replace();

    const analyzeForm = document.getElementById('analyzeForm');
    const repoInputContainer = document.getElementById('repoInputContainer');
    const pasteCodeContainer = document.getElementById('pasteCodeContainer');
    const repoUrlInput = document.getElementById('repoUrl');
    const pastedCodeInput = document.getElementById('pastedCode');
    const codeLanguage = document.getElementById('codeLanguage');

    const toggleRepo = document.getElementById('toggleRepo');
    const toggleCode = document.getElementById('toggleCode');

    const pages = {
        home: document.getElementById('homePage'),
        dashboard: document.getElementById('dashboardPage'),
        history: document.getElementById('historyPage'),
        certificate: document.getElementById('certificatePage')
    };

    let currentReviewData = null;
    let loadingInterval = null;
    let currentAbortController = null;

    const themeToggle = document.getElementById('themeToggle');
    const themeIcon = document.getElementById('themeIcon');
    const isLight = localStorage.getItem('theme') === 'light';
    if(isLight) {
        document.body.classList.add('light-theme');
        themeIcon.setAttribute('data-feather', 'moon');
        if(typeof feather !== 'undefined') feather.replace();
    }

    themeToggle?.addEventListener('click', () => {
        document.body.classList.toggle('light-theme');
        const light = document.body.classList.contains('light-theme');
        localStorage.setItem('theme', light ? 'light' : 'dark');
        themeIcon.setAttribute('data-feather', light ? 'moon' : 'sun');
        if(typeof feather !== 'undefined') feather.replace();
    });

    document.getElementById('navBrand')?.addEventListener('click', (e) => {
        document.getElementById('navHome').click();
    });
    document.getElementById('navHome')?.addEventListener('click', (e) => {
        e.preventDefault();
        Object.values(pages).forEach(p => p.classList.add('hidden'));
        pages.certificate.classList.remove('active');
        pages.home.classList.remove('hidden');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    document.getElementById('navFeatures')?.addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('navHome').click();
        setTimeout(() => document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' }), 100);
    });
    document.getElementById('navHow')?.addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('navHome').click();
        setTimeout(() => document.getElementById('how-it-works')?.scrollIntoView({ behavior: 'smooth' }), 100);
    });
    document.getElementById('navHistory')?.addEventListener('click', (e) => {
        e.preventDefault();
        Object.values(pages).forEach(p => p.classList.add('hidden'));
        pages.certificate.classList.remove('active');
        pages.history.classList.remove('hidden');
        loadHistory();
    });

    toggleRepo?.addEventListener('click', (e) => {
        e.preventDefault();
        toggleRepo.classList.add('active');
        toggleCode.classList.remove('active');
        repoInputContainer.style.display = 'flex';
        pasteCodeContainer.style.display = 'none';
        repoUrlInput.disabled = false;
        pastedCodeInput.disabled = true;
    });

    toggleCode?.addEventListener('click', (e) => {
        e.preventDefault();
        toggleCode.classList.add('active');
        toggleRepo.classList.remove('active');
        repoInputContainer.style.display = 'none';
        pasteCodeContainer.style.display = 'block';
        repoUrlInput.disabled = true;
        pastedCodeInput.disabled = false;
    });

    function startLoadingAnimation(text) {
        document.getElementById('loadingRepoName').textContent = text;
        const steps = document.querySelectorAll('#loadingSteps .step');
        const progressCircle = document.getElementById('loadingProgressCircle');
        const pctText = document.getElementById('loadingProgressValue');

        steps.forEach(s => {
            s.classList.remove('active', 'done');
            s.classList.add('pending');
            s.querySelector('.step-status').textContent = 'Pending';
        });

        if(progressCircle) progressCircle.style.strokeDashoffset = '339.292';
        if(pctText) pctText.textContent = '0%';

        document.getElementById('loadingOverlay').classList.remove('hidden');

        let progress = 0;
        let stepIdx = 0;
        if(steps.length > 0) {
            steps[stepIdx].classList.remove('pending');
            steps[stepIdx].classList.add('active');
            steps[stepIdx].querySelector('.step-status').textContent = 'In progress...';
        }

        loadingInterval = setInterval(() => {
            if(progress >= 95) return;
            progress += Math.random() * 5;
            if(progress > 95) progress = 95;

            if(progressCircle) progressCircle.style.strokeDashoffset = 339.292 - (339.292 * (progress/100));
            if(pctText) pctText.textContent = Math.floor(progress) + '%';

            let expectedStep = Math.floor(progress / 20);
            if(expectedStep > stepIdx && expectedStep < steps.length) {
                steps[stepIdx].classList.remove('active');
                steps[stepIdx].classList.add('done');
                steps[stepIdx].querySelector('.step-status').textContent = 'Completed';
                stepIdx = expectedStep;
                steps[stepIdx].classList.remove('pending');
                steps[stepIdx].classList.add('active');
                steps[stepIdx].querySelector('.step-status').textContent = 'In progress...';
            }
        }, 800);
    }

    function stopLoadingAnimation() {
        clearInterval(loadingInterval);
        document.getElementById('loadingOverlay').classList.add('hidden');
    }

    function completeLoadingAnimation(callback) {
        clearInterval(loadingInterval);
        const steps = document.querySelectorAll('#loadingSteps .step');
        const progressCircle = document.getElementById('loadingProgressCircle');
        const pctText = document.getElementById('loadingProgressValue');

        let progress = parseInt(pctText.textContent) || 0;

        let finishInterval = setInterval(() => {
            progress += 5;
            if(progress >= 100) {
                progress = 100;
                clearInterval(finishInterval);

                steps.forEach(s => {
                    s.classList.remove('pending', 'active');
                    s.classList.add('done');
                    s.querySelector('.step-status').textContent = 'Completed';
                });

                setTimeout(() => {
                    document.getElementById('loadingOverlay').classList.add('hidden');
                    if(callback) callback();
                }, 500); 
            }
            if(progressCircle) progressCircle.style.strokeDashoffset = 339.292 - (339.292 * (progress/100));
            if(pctText) pctText.textContent = Math.floor(progress) + '%';
        }, 20);
    }

    document.getElementById('cancelAnalysisBtn')?.addEventListener('click', () => {
        if(currentAbortController) {
            currentAbortController.abort();
            currentAbortController = null;
        }
        stopLoadingAnimation();
    });

    analyzeForm?.addEventListener('submit', async (e) => {
        e.preventDefault();

        let endpoint, bodyParams, loadingText;
        if (toggleRepo.classList.contains('active')) {
            let url = repoUrlInput.value.trim();
            if (!url) return;
            if (!url.startsWith('http://') && !url.startsWith('https://')) url = 'https://' + url;
            endpoint = '/api/reviews';
            bodyParams = { repositoryUrl: url };
            loadingText = url;
        } else {
            let code = pastedCodeInput.value.trim();
            if (!code) return;
            endpoint = '/api/reviews/paste';
            bodyParams = { code: code, language: codeLanguage.value };
            loadingText = 'Pasted Code';
        }

        startLoadingAnimation(loadingText);

        currentAbortController = new AbortController();
        try {
            const res = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bodyParams),
                signal: currentAbortController.signal
            });

            if(!res.ok) throw new Error(await res.text() || 'Failed to analyze');
            const data = await res.json();

            completeLoadingAnimation(() => populateDashboard(data));

        } catch (err) {
            stopLoadingAnimation();
            if (err.name === 'AbortError') return;
            alert("Error: " + err.message);
        }
    });

    function escapeHtml(unsafe) {
        if(!unsafe) return '';
        return unsafe.toString()
             .replace(/&/g, "&amp;")
             .replace(/</g, "&lt;")
             .replace(/>/g, "&gt;")
             .replace(/"/g, "&quot;")
             .replace(/'/g, "&#039;");
    }

    // Export function to global scope so HTML can access it for onclick attributes
    window.copyToClipboard = function(text, btn) {
        navigator.clipboard.writeText(text);
        btn.innerHTML = 'Copied!';
        setTimeout(() => {
            btn.innerHTML = '<i data-feather="copy"></i>';
            if(typeof feather !== 'undefined') feather.replace();
        }, 2000);
    };

    function populateDashboard(data) {
        currentReviewData = data;
        Object.values(pages).forEach(p => p.classList.add('hidden'));
        pages.certificate.classList.remove('active');
        pages.dashboard.classList.remove('hidden');

        const isPaste = data.sourceType === 'PASTED_CODE';

        document.getElementById('dashRepoName').textContent = data.repositoryName || 'Analysis';
        document.getElementById('dashRepoUrl').textContent = data.repositoryUrl || 'Local Code';
        document.getElementById('dashBranch').textContent = data.branch || 'main';
        document.getElementById('dashDate').textContent = new Date(data.createdAt).toLocaleString();

        const score = data.score || 0;
        document.getElementById('dashScore').textContent = score;
        const scoreCircle = document.getElementById('dashScoreCircle');
        scoreCircle.style.strokeDashoffset = 339.292 - (339.292 * (score/100));
        scoreCircle.style.stroke = score >= 80 ? 'var(--success)' : score >= 50 ? 'var(--medium)' : 'var(--critical)';

        const badge = document.getElementById('dashScoreBadge');
        badge.textContent = score >= 80 ? 'Good' : score >= 50 ? 'Needs Work' : 'Critical';
        badge.style.color = scoreCircle.style.stroke;
        badge.style.background = score >= 80 ? 'rgba(16,185,129,0.1)' : score >= 50 ? 'rgba(234,179,8,0.1)' : 'rgba(239,68,68,0.1)';

        document.getElementById('statTotal').textContent = data.totalIssues || 0;
        document.getElementById('statCritical').textContent = data.criticalIssues || 0;
        document.getElementById('statHigh').textContent = data.highIssues || 0;
        document.getElementById('statMedium').textContent = data.mediumIssues || 0;

        document.getElementById('statFiles').textContent = data.filesAnalyzed || (isPaste ? 1 : 0);
        document.getElementById('statLines').textContent = data.linesOfCode || 0;
        document.getElementById('statLang').textContent = data.language || 'Unknown';
        document.getElementById('statFramework').textContent = data.framework || 'None';

        const sec = Math.floor((data.analysisTimeMs || 0) / 1000);
        document.getElementById('statTime').textContent = sec > 0 ? (sec + 's') : '< 1s';

        document.getElementById('dashSummary').textContent = data.summary || 'Review completed successfully.';

        // Populate Files Tree
        if(!isPaste && data.analyzedFiles && data.analyzedFiles.length > 0) {
            renderFileTree(data.analyzedFiles.map(f => ({path: f})));
        } else if (!isPaste) {
            const filesMap = {};
            (data.issues || []).forEach(iss => {
                if(iss.file && iss.file !== 'Unknown') filesMap[iss.file] = {path: iss.file};
            });
            renderFileTree(Object.values(filesMap));
        } else {
            renderFileTree([{ path: 'PastedCode.' + (data.language==='Python'?'py':data.language==='JavaScript'?'js':'java'), content: pastedCodeInput.value }]);
        }

        renderSplitPanes(data.issues || []);

        // Re-analyze button
        document.getElementById('btnReanalyze').onclick = () => {
            if (isPaste) {
                toggleCode.click();
            } else {
                toggleRepo.click();
                repoUrlInput.value = data.repositoryUrl;
            }
            pages.dashboard.classList.add('hidden');
            pages.home.classList.remove('hidden');
        };

        if(typeof feather !== 'undefined') feather.replace();
    }

    // Tabs logic
    document.querySelectorAll('.dash-tab').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.dash-tab').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            e.target.classList.add('active');
            document.getElementById('tab-' + e.target.dataset.tab).classList.add('active');
        });
    });

    document.querySelectorAll('.fm-tab').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.fm-tab').forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            if(e.target.dataset.ftab === 'code') {
                document.getElementById('fmCodeView').style.display = 'block';
                document.getElementById('fmIssuesView').style.display = 'none';
            } else {
                document.getElementById('fmCodeView').style.display = 'none';
                document.getElementById('fmIssuesView').style.display = 'block';
            }
        });
    });

    function renderFileTree(files) {
        const tree = document.getElementById('fsTree');
        tree.innerHTML = '';
        document.getElementById('fsFileCount').textContent = `${files.length} files`;

        files.forEach(f => {
            const div = document.createElement('div');
            div.className = 'fs-item';
            div.innerHTML = `<i data-feather="file" style="width:14px;"></i> ${f.path.split('/').pop()}`;
            div.title = f.path;

            const fileIssues = (currentReviewData.issues || []).filter(i => i.file && i.file.includes(f.path.split('/').pop()));
            if (fileIssues.length > 0) {
                const dot = document.createElement('div');
                dot.className = 'fs-issue-dot';
                div.appendChild(dot);
            }

            div.onclick = () => {
                document.querySelectorAll('.fs-item').forEach(i => i.classList.remove('active'));
                div.classList.add('active');
                openFile(f, fileIssues);
            };
            tree.appendChild(div);
        });

        // Auto-select first file
        if(tree.firstChild) tree.firstChild.click();

        if(typeof feather !== 'undefined') feather.replace();
    }

    async function openFile(file, issues) {
        document.getElementById('fmFileName').textContent = file.path;
        const codeView = document.getElementById('fmCodeView');
        codeView.innerHTML = '<div style="color:var(--text-muted); padding:16px;">Loading code...</div>';

        let content = file.content;

        if (!content && currentReviewData.sourceType !== 'PASTED_CODE') {
            // Fetch dynamically from raw github
            try {
                let rUrl = currentReviewData.repositoryUrl.replace('https://github.com/', '').replace('http://github.com/', '').replace('.git', '');
                let urlParts = rUrl.split('/');
                let owner = urlParts[0];
                let repo = urlParts[1];
                let branch = currentReviewData.branch || 'main';
                let rawUrl = `https://raw.githubusercontent.com/${owner}/${repo}/${branch}/${file.path}`;

                const res = await fetch(rawUrl);
                if(res.ok) {
                    content = await res.text();
                } else {
                    content = "
                }
            } catch(e) {
                content = "// Network error while loading source code.";
            }
        }

        codeView.innerHTML = '';

        if (content) {
            const lines = content.split('\n');
            lines.forEach((line, i) => {
                const lineNum = i + 1;
                const lineDiv = document.createElement('div');
                lineDiv.className = 'code-line';
                lineDiv.id = 'line-' + lineNum;

                const hasIssue = issues.some(iss => iss.line === lineNum);
                if(hasIssue) lineDiv.classList.add('has-issue');

                lineDiv.innerHTML = `<div class="code-num">${lineNum}</div><div class="code-content">${escapeHtml(line)}</div>`;
                codeView.appendChild(lineDiv);
            });
        }

        const issuesView = document.getElementById('fmIssuesView');
        issuesView.innerHTML = issues.length === 0 ? '<p style="color:var(--text-muted);">No issues found in this file.</p>' : '';
        issues.forEach(iss => {
            const card = createIssueCard(iss);
            issuesView.appendChild(card);
        });
    }

    function createIssueCard(iss) {
        const div = document.createElement('div');
        div.className = 'issue-card-premium';
        let badgeClass = iss.severity === 'CRITICAL' ? 'badge-critical' : iss.severity === 'HIGH' ? 'badge-high' : iss.severity === 'MEDIUM' ? 'badge-medium' : 'badge-low';

        let safeCodeSnippet = escapeHtml(iss.codeSnippet || '');
        let safeRawSnippet = (iss.codeSnippet || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, "&quot;").replace(/\n/g, '\\n');

        let safeFixSnippet = escapeHtml(iss.suggestedFix || '');
        let safeRawFix = (iss.suggestedFix || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, "&quot;").replace(/\n/g, '\\n');

        div.innerHTML = `
            <div class="ic-top">
                <div class="ic-badges">
                    <span class="ic-badge ${badgeClass}">${iss.severity}</span>
                    <span class="ic-badge badge-cat">${iss.category}</span>
                </div>
            </div>
            <div class="ic-title">${escapeHtml(iss.title)}</div>
            <div class="ic-loc">
                <span><i data-feather="file" style="width:12px; margin-right:4px;"></i>${escapeHtml(iss.file || 'Unknown')}</span>
                <span>Line ${iss.line || 'N/A'}</span>
            </div>
            ${iss.codeSnippet ? `
            <div class="ic-code-box">
                <button class="ic-copy" onclick="window.copyToClipboard('${safeRawSnippet}', this)"><i data-feather="copy"></i></button>
                <code>${safeCodeSnippet}</code>
            </div>` : ''}
            <div class="ic-section">
                <h4>Description</h4>
                <p>${escapeHtml(iss.description)}</p>
            </div>
            ${iss.recommendation ? `
            <div class="ic-section">
                <h4>Recommendation</h4>
                <p>${escapeHtml(iss.recommendation)}</p>
            </div>` : ''}
            ${iss.suggestedFix ? `
            <div class="ic-section">
                <h4>Suggested Fix</h4>
                <div class="ic-code-box" style="border-color:var(--success);">
                    <button class="ic-copy" onclick="window.copyToClipboard('${safeRawFix}', this)"><i data-feather="copy"></i></button>
                    <code style="color:#d4d4d8;">${safeFixSnippet}</code>
                </div>
            </div>` : ''}
        `;
        return div;
    }

    function renderSplitPanes(issues) {

        const isTree = document.getElementById('isTree');
        const isDetailView = document.getElementById('isDetailView');
        isTree.innerHTML = '';
        document.getElementById('isIssueCount').textContent = `${issues.length} issues`;

        const secIssues = issues.filter(i => i.category && i.category.toUpperCase() === 'SECURITY');
        const secTree = document.getElementById('secTree');
        const secDetailView = document.getElementById('secDetailView');
        secTree.innerHTML = '';
        document.getElementById('secIssueCount').textContent = `${secIssues.length} findings`;
        if(secIssues.length === 0) secDetailView.innerHTML = '<p style="color:var(--text-muted); text-align:center; margin-top:100px;">No security issues detected.</p>';

        function createTreeItem(iss, tree, detailView) {
            const div = document.createElement('div');
            div.className = 'fs-item';
            div.innerHTML = `
                <div style="display:flex; flex-direction:column; gap:4px; width:100%;">
                    <div style="display:flex; justify-content:space-between; align-items:center;">
                        <span style="font-weight:600; color:var(--text-main); font-family:var(--font-sans);">${escapeHtml(iss.title)}</span>
                        <div class="fs-issue-dot" style="background:var(--${iss.severity.toLowerCase()});"></div>
                    </div>
                    <div style="font-size:11px; color:var(--text-muted);"><i data-feather="file" style="width:10px;"></i> ${escapeHtml(iss.file || 'Global')}</div>
                </div>
            `;
            div.onclick = () => {
                Array.from(tree.children).forEach(c => c.classList.remove('active'));
                div.classList.add('active');
                detailView.innerHTML = '';
                detailView.appendChild(createIssueCard(iss));
                if(typeof feather !== 'undefined') feather.replace();
            };
            tree.appendChild(div);
        }

        issues.forEach(iss => createTreeItem(iss, isTree, isDetailView));
        secIssues.forEach(iss => createTreeItem(iss, secTree, secDetailView));

        if(isTree.firstChild) isTree.firstChild.click();
        if(secTree.firstChild) secTree.firstChild.click();

        const sugContainer = document.getElementById('sugCardContainer');
        if (sugContainer) {
            sugContainer.innerHTML = '';
            const newSugIssues = issues.filter(i => i.recommendation || i.snippet);
            if (newSugIssues.length === 0) {
                sugContainer.innerHTML = '<div style="color:var(--text-muted); padding:32px; text-align:center; border:1px dashed var(--border-color); border-radius:var(--radius-md);">No code suggestions available for this repository.</div>';
            } else {
                newSugIssues.forEach(iss => {
                    const card = document.createElement('div');
                    card.className = 'sug-card';
                    let fixHtml = '';
                    if (iss.snippet) {
                        fixHtml = '<div class="sug-card-fix">' + escapeHtml(iss.snippet) + '</div>';
                    }
                    card.innerHTML = `
                        <div class="sug-card-header">
                            <div>
                                <div class="sug-card-title">${escapeHtml(iss.title)}</div>
                                <div class="sug-card-meta"><i data-feather="file" style="width:12px;"></i> ${escapeHtml(iss.file || 'Global')} ${iss.line ? '· Line ' + iss.line : ''}</div>
                            </div>
                        </div>
                        <div class="sug-card-desc">${escapeHtml(iss.recommendation || iss.description || 'Review the suggested fix.')}</div>
                        ${fixHtml}
                        <div class="sug-card-actions">
                            <button class="dh-btn" style="background:transparent; border:1px solid var(--border-color); color:var(--text-main);"><i data-feather="copy"></i> Copy Fix</button>
                            <button class="dh-btn" style="background:var(--text-main); color:var(--bg-main); border:none;"><i data-feather="code"></i> Apply Fix</button>
                        </div>
                    `;
                    sugContainer.appendChild(card);
                });
            }
        }

        document.getElementById('isFilterSelect').onchange = (e) => {
            const val = e.target.value;
            Array.from(isTree.children).forEach((node, idx) => {
                if (val === 'All' || issues[idx].severity === val) node.style.display = 'block';
                else node.style.display = 'none';
            });

            const firstVisible = Array.from(isTree.children).find(n => n.style.display !== 'none');
            if(firstVisible) firstVisible.click();
            else isDetailView.innerHTML = '<p style="color:var(--text-muted); text-align:center; margin-top:100px;">No issues match filter</p>';
        };
    }

    document.getElementById('btnCertificate')?.addEventListener('click', () => {
        Object.values(pages).forEach(p => p.classList.add('hidden'));
        pages.certificate.classList.add('active');

        document.getElementById('cRepoName').textContent = currentReviewData.repositoryName || 'Pasted Code';
        document.getElementById('cRepoUrl').textContent = currentReviewData.repositoryUrl || 'N/A';
        document.getElementById('cDate').textContent = new Date(currentReviewData.createdAt).toLocaleString();
        document.getElementById('cId').textContent = currentReviewData.id || 'TEMP-' + Math.floor(Math.random()*10000);
        document.getElementById('cScore').textContent = currentReviewData.score || 0;
        document.getElementById('cFiles').textContent = currentReviewData.filesAnalyzed || 1;
        document.getElementById('cIssues').textContent = currentReviewData.totalIssues || 0;
    });

    document.getElementById('btnCertBack')?.addEventListener('click', () => {
        pages.certificate.classList.remove('active');
        pages.dashboard.classList.remove('hidden');
    });

    document.getElementById('btnCertPrint')?.addEventListener('click', () => {
        window.print();
    });

    async function loadHistory() {
        const grid = document.getElementById('historyGrid');
        grid.innerHTML = '<div style="color:var(--text-muted);">Loading history...</div>';
        try {
            const res = await fetch('/api/reviews');
            const data = await res.json();
            grid.innerHTML = '';

            if(data.length === 0) {
                grid.innerHTML = '<div style="color:var(--text-muted);">No previous analyses found.</div>';
                return;
            }

            data.sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt)).forEach(r => {
                const isPaste = r.sourceType === 'PASTED_CODE';
                const div = document.createElement('div');
                div.className = 'history-card';
                div.innerHTML = `
                    <div class="hc-title">
                        <i data-feather="${isPaste ? 'file-text' : 'github'}"></i>
                        ${escapeHtml(r.repositoryName || (isPaste ? 'Pasted Code' : 'Unknown'))}
                    </div>
                    <div class="hc-stats">
                        <div class="hc-stat"><i data-feather="activity" style="width:14px; color:var(--success);"></i> Score ${r.score||0}</div>
                        <div class="hc-stat"><i data-feather="alert-circle" style="width:14px; color:var(--high);"></i> ${r.totalIssues||0} Issues</div>
                        <div class="hc-stat"><i data-feather="shield" style="width:14px; color:var(--critical);"></i> ${r.criticalIssues||0} Critical</div>
                    </div>
                    <div class="hc-foot">
                        <span>${escapeHtml(r.language || 'Unknown')}</span>
                        <span>${new Date(r.createdAt).toLocaleDateString()}</span>
                    </div>
                `;
                div.onclick = () => {
                    populateDashboard(r);
                };
                grid.appendChild(div);
            });
            if(typeof feather !== 'undefined') feather.replace();
        } catch (e) {
            grid.innerHTML = '<div style="color:var(--critical);">Failed to load history.</div>';
        }
    }
});